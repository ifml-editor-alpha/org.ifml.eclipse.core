package org.ifml.eclipse.core.resources;

import java.io.FilterOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.ifml.base.Objects2;
import org.ifml.eclipse.core.runtime.ProgressMonitors;
import org.ifml.eclipse.core.runtime.Statuses;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.FileBackedOutputStream;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

/**
 * Provides utility methods for the workspace.
 */
public class Workspaces {

    private static final boolean CASE_SENSITIVE = Platform.OS_MACOSX.equals(Platform.getOS()) ? false : new java.io.File("a")
            .compareTo(new java.io.File("A")) != 0;

    private Workspaces() {
    }

    /**
     * Returns whether the workspaces is case sensitive.
     * 
     * @return {@code true} if the workspaces is case sensitive.
     */
    public static boolean isCaseSensitive() {
        return CASE_SENSITIVE;
    }

    /**
     * Returns the root resource of the workspace.
     * 
     * @return the workspace root.
     */
    public static IWorkspaceRoot getRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    /**
     * Removes all projects in the workspace.
     * 
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if the removal fails.
     */
    public static void clearWorkspace(IProgressMonitor monitor) throws CoreException {
        monitor = ProgressMonitors.monitorFor(monitor);
        try {
            IProject[] projects = getRoot().getProjects();
            if (projects.length > 0) {
                monitor.beginTask("Removing all workspace projects", projects.length);
                for (IProject project : projects) {
                    project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT,
                            ProgressMonitors.subMonitorFor(monitor, 1));
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Returns the workspace projects that satisfy a predicate. All projects are returned in case of null predicate.
     * 
     * @param predicate
     *            a predicate that determines whether a project should be returned; {@code null} is equivalent to a predicate that
     *            always return {@code true}.
     * @return the projects that satisfy {@code predicate}.
     */
    public static Iterable<IProject> getProjects(@Nullable Predicate<? super IProject> predicate) {
        if (predicate == null) {
            predicate = Predicates.alwaysTrue();
        }
        return Iterables.filter(Arrays.asList(getRoot().getProjects()), predicate);
    }

    /**
     * Gets the explicit or implicit default charset of a container, limiting implicit charset inheritance to a specified context
     * container.
     * 
     * @param container
     *            container for which to get the charset
     * @param context
     *            context for implicit charset search or <code>null</code> to use the Workspace itself like
     * @return an encoding name or <code>null</code> to use the runtime platform encoding
     * @throws CoreException
     *             if this method fails.
     */
    public static String getContextualDefaultCharset(IContainer container, IContainer context) throws CoreException {
        while (container != null && container.exists()) {
            String charset = container.getDefaultCharset(false);
            if (charset != null) {
                return charset;
            }
            if (context != null && context.equals(container)) {
                break;
            }
            container = container.getParent();
        }
        return null;
    }

    /**
     * Returns a factory that will supply instances of {@link InputStream} that read from a workspace file.
     * 
     * @param file
     *            the workspace file to read from
     * @return the factory.
     */
    public static InputSupplier<InputStream> newInputStreamSupplier(final IFile file) {
        return new InputSupplier<InputStream>() {

            @Override
            public InputStream getInput() throws IOException {
                try {
                    return file.getContents();
                } catch (CoreException e) {
                    IOException ioExc = new IOException(e.toString());
                    ioExc.initCause(e);
                    throw ioExc;
                }
            }

        };
    }

    /**
     * Returns a factory that will supply instances of {@link Reader} that read from a workspace file, using the file's charset.
     * 
     * @param file
     *            the workspace file to read from.
     * @return the factory.
     */
    public static InputSupplier<Reader> newReaderSupplier(final IFile file) {
        return newReaderSupplier(file, null);
    }

    /**
     * Returns a factory that will supply instances of {@link Reader} that read from a workspace file, using a specific charset.
     * 
     * @param file
     *            the workspace file to read from.
     * @param charset
     *            the charset used to transform bytes into characters or {@code null} to use the file's charset.
     * @return the factory.
     */
    public static InputSupplier<Reader> newReaderSupplier(final IFile file, @Nullable final String charset) {
        return new InputSupplier<Reader>() {

            @Override
            public Reader getInput() throws IOException {
                try {
                    return new InputStreamReader(file.getContents(), charset != null ? charset : file.getCharset());
                } catch (CoreException e) {
                    IOException ioExc = new IOException(e.toString());
                    ioExc.initCause(e);
                    throw ioExc;
                }
            }

        };
    }

    /**
     * Returns a factory that will supply instances of {@link OutputStream} to write on a workspace file.
     * 
     * @param file
     *            the workspace file to write into.
     * @return the factory.
     */
    public static OutputSupplier<OutputStream> newOutputStreamSupplier(final IFile file) {
        return new OutputSupplier<OutputStream>() {

            @Override
            public OutputStream getOutput() throws IOException {
                final FileBackedOutputStream backedOut = new FileBackedOutputStream(1024 * 1024, false);
                return new FilterOutputStream(backedOut) {

                    private boolean closed;

                    @Override
                    public void close() throws IOException {
                        if (!closed) {
                            InputStream in = null;
                            try {
                                in = backedOut.getSupplier().getInput();
                                if (file.exists()) {
                                    file.setContents(in, true, true, ProgressMonitors.emptyMonitor());
                                } else {
                                    file.create(in, true, ProgressMonitors.emptyMonitor());
                                }
                            } catch (CoreException e) {
                                IOException ioExc = new IOException(e.toString());
                                ioExc.initCause(e);
                                throw ioExc;
                            } finally {
                                closed = true;
                                Closeables.closeQuietly(in);
                                backedOut.reset();
                                super.close();
                            }
                        }
                    }
                };
            }
        };
    }

    /**
     * Returns a factory that will supply instances of {@link Writer} to write on a workspace file, using the file's charset.
     * 
     * @param file
     *            the workspace file to write into.
     * @return the factory.
     */
    public static OutputSupplier<Writer> newWriterSupplier(IFile file) {
        return newWriterSupplier(file, null);
    }

    /**
     * Returns a factory that will supply instances of {@link Writer} to write on a workspace file, using a specific charset.
     * 
     * @param file
     *            the workspace file to write into.
     * @param charset
     *            the charset used to encode characters into bytes or {@code null} to use the file's charset.
     * @return the factory.
     */
    public static OutputSupplier<Writer> newWriterSupplier(final IFile file, @Nullable final String charset) {
        return new OutputSupplier<Writer>() {

            @Override
            public Writer getOutput() throws IOException {
                @SuppressWarnings("resource")
                final FileBackedOutputStream backedOut = new FileBackedOutputStream(1024 * 1024, false);
                try {
                    return new FilterWriter(new OutputStreamWriter(backedOut, charset != null ? charset : file.getCharset())) {

                        private boolean closed;

                        @Override
                        public void close() throws IOException {
                            if (!closed) {
                                InputStream in = null;
                                try {
                                    in = backedOut.getSupplier().getInput();
                                    if (file.exists()) {
                                        file.setContents(in, true, true, ProgressMonitors.emptyMonitor());
                                    } else {
                                        file.create(in, true, ProgressMonitors.emptyMonitor());
                                    }
                                } catch (CoreException e) {
                                    IOException ioExc = new IOException(e.toString());
                                    ioExc.initCause(e);
                                    throw ioExc;
                                } finally {
                                    closed = true;
                                    Closeables.closeQuietly(in);
                                    backedOut.reset();
                                }
                            }
                        }
                    };
                } catch (CoreException e) {
                    IOException ioExc = new IOException(e.toString());
                    ioExc.initCause(e);
                    throw ioExc;
                }
            }
        };
    }

    /**
     * Creates (or refreshes) a workspace project.
     * 
     * @param project
     *            the project to create or refresh.
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if this method fails.
     */
    public static void createOrRefresh(IProject project, IProgressMonitor monitor) throws CoreException {
        Preconditions.checkNotNull(project);
        monitor = ProgressMonitors.monitorFor(monitor);
        if (!project.exists()) {
            project.create(monitor);
        } else {
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    /**
     * Creates (or refreshes) a workspace folder.
     * 
     * @param folder
     *            the folder to create or refresh.
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if this method fails.
     */
    public static void createOrRefresh(IFolder folder, IProgressMonitor monitor) throws CoreException {
        Preconditions.checkNotNull(folder);
        monitor = ProgressMonitors.monitorFor(monitor);
        if (!folder.exists()) {
            folder.create(true, true, monitor);
        } else {
            folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    /**
     * Writes the contents of a workspace file.
     * 
     * @param file
     *            the file to create or replace.
     * @param from
     *            the input stream.
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if this method fails.
     */
    public static void writeFile(IFile file, InputSupplier<? extends InputStream> from, IProgressMonitor monitor) throws CoreException {
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(from);
        monitor = ProgressMonitors.monitorFor(monitor);
        InputStream in = null;
        try {
            in = from.getInput();
            if (file.exists()) {
                file.setContents(in, true, true, monitor);
            } else {
                file.create(in, true, monitor);
            }
        } catch (IOException e) {
            throw new CoreException(Statuses.getErrorStatus(e, null, null));
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    /**
     * Gets the explicit or implicit default charset of a file, limiting implicit charset inheritance to a specified context container.
     * 
     * @param file
     *            file for which to get the charset
     * @param context
     *            context for implicit charset search or <code>null</code> to use the Workspace itself like
     * @return an encoding name or <code>null</code> to use the runtime platform encoding
     * @throws CoreException
     *             if this method fails.
     */
    public static String getContextualDefaultCharset(IFile file, IContainer context) throws CoreException {
        if (file != null && file.exists()) {
            String charset = file.getCharset(false);
            if (charset != null) {
                return charset;
            } else {
                return getContextualDefaultCharset(file.getParent(), context);
            }
        }
        return null;
    }

    /**
     * Creates a folder, including any necessary but nonexistent parent folders.
     * 
     * @param folder
     *            the folder to create.
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if this method fails.
     */
    public static final void createFolders(IFolder folder, IProgressMonitor monitor) throws CoreException {
        monitor = ProgressMonitors.monitorFor(monitor);
        try {
            monitor.beginTask("Creating folder hierarchy for '" + folder.getName() + "'", ProgressMonitors.DEFAULT_TOTAL_WORK);

            /* computes the list of folders to create */
            List<IFolder> foldersToCreate = Lists.newArrayList();
            IResource resource = folder;
            while (resource != null) {
                if ((resource instanceof IFolder) && !resource.exists()) {
                    foldersToCreate.add((IFolder) resource);
                }
                resource = resource.getParent();
            }

            /* creates the folders in reverse order */
            for (IFolder folderToCreate : Lists.reverse(foldersToCreate)) {
                folderToCreate.create(true, true, ProgressMonitors.emptyMonitor());
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Creates the parent folder of a file, including any necessary but nonexistent ancestor folders.
     * 
     * @param file
     *            the file whose ancestor folder hierarchy must be created.
     * @param monitor
     *            a progress monitor.
     * @throws CoreException
     *             if this method fails.
     */
    public static final void createFolders(IFile file, IProgressMonitor monitor) throws CoreException {
        IContainer parent = file.getParent();
        if (parent instanceof IFolder) {
            createFolders((IFolder) parent, monitor);
        }
    }

    /**
     * Returns the folders included in a container.
     * 
     * @param container
     *            a container.
     * @return the folders.
     * @throws CoreException
     *             if this method fails.
     */
    public static Iterable<IFolder> getFolders(IContainer container) throws CoreException {
        IResource[] members = container.members();
        List<IFolder> folders = Lists.newArrayList();
        for (IResource member : members) {
            if (member instanceof IFolder) {
                folders.add((IFolder) member);
            }
        }
        return ImmutableList.copyOf(folders);
    }

    /**
     * Returns the first resource included into a container having a specific name, ignoring case.
     * 
     * @param container
     *            the parent container.
     * @param name
     *            the name to search for.
     * @return the first child resource matching {@code name}, or {@code null} if not found.
     * @throws CoreException
     *             if this method fails.
     */
    public static final IResource getMemberIgnoringCase(IContainer container, String name) throws CoreException {
        Preconditions.checkArgument(!name.contains("/"));
        for (IResource resource : container.members()) {
            if (resource.getName().equalsIgnoreCase(name)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Returns the {@link ISchedulingRule} that is required either to create or modify a resource.
     * 
     * @param resource
     *            the resource whose create (if it does not exist) or modify (if it exists) rule must be returned.
     * @return the scheduling rule.
     */
    public static ISchedulingRule getWriteSchedulingRule(IResource resource) {
        if (resource.exists()) {
            return resource.getWorkspace().getRuleFactory().modifyRule(resource);
        } else {
            return resource.getWorkspace().getRuleFactory().createRule(resource);
        }
    }

    /**
     * Scans a set of resources searching for the resources which really exist in the workspace.
     * 
     * @param resources
     *            the set of resources to scan.
     * @param includeAncestors
     *            if {@code true} also the ancestores of existing resources are included in the result.
     * @return the set of existing resources.
     */
    public static Set<IResource> getExistingResources(Iterable<? extends IResource> resources, boolean includeAncestors) {
        Set<IResource> result = Sets.newHashSet();
        for (IResource resource : resources) {
            if (includeAncestors) {
                IResource tempResource = resource;
                while ((tempResource != null) && tempResource.exists()) {
                    result.add(tempResource);
                    tempResource = tempResource.getParent();
                }
            } else {
                if (resource.exists()) {
                    result.add(resource);
                }
            }
        }
        return result;
    }

    /**
     * Scans the workspace for a set of existing resources having the specified paths.
     * 
     * @param paths
     *            the set of paths to search for.
     * @return the set of existing resources.
     */
    public static Set<IResource> getExistingResources(Iterable<? extends IPath> paths) {
        if (paths == null) {
            return ImmutableSet.of();
        }
        Set<IResource> resources = Sets.newHashSet();
        IWorkspaceRoot root = Workspaces.getRoot();
        for (IPath path : paths) {
            IResource res = root.findMember(path);
            if (res != null) {
                resources.add(res);
            }
        }
        return ImmutableSet.copyOf(resources);
    }

    /**
     * Finds a case variant of a specific resource.
     * 
     * @param res
     *            the resource to match.
     * @param acceptExactMatch
     *            if {@code true} the resource itself is returned if exists; if {@code false} a {@code null} value is returned if the
     *            resource itself exists.
     * @return the case variant resource or {@code null}.
     * @throws CoreException
     *             if an error occurred.
     */
    public static IResource findCaseVariant(IResource res, boolean acceptExactMatch) throws CoreException {
        if (res.exists()) {
            return acceptExactMatch ? res : null;
        }
        String name = res.getName();
        IContainer parent = Objects2.as(findCaseVariant(res.getParent(), true), IContainer.class);
        if (parent != null) {
            if (parent.exists()) {
                for (IResource siblingRes : parent.members()) {
                    if (siblingRes.getName().equalsIgnoreCase(name)) {
                        return siblingRes;
                    }
                }
            }
            if (parent instanceof IWorkspaceRoot) {
                return ((IWorkspaceRoot) parent).getProject(name);
            } else if ((parent instanceof IProject) || (parent instanceof IFolder)) {
                if (res instanceof IFile) {
                    return parent.getFile(new Path(name));
                } else if (res instanceof IFolder) {
                    return parent.getFolder(new Path(name));
                }
            }
        }
        return null;
    }

    /**
     * Returns a comparator able to compare the workspace-relative path of two {@link IResource}s.
     * 
     * @return a resource path comparator.
     */
    public static final Comparator<IResource> getResourcePathComparator() {
        return ResourcePathComparator.INSTANCE;
    }

    private static enum ResourcePathComparator implements Comparator<IResource> {

        INSTANCE;

        @Override
        public int compare(IResource res1, IResource res2) {
            return res1.getFullPath().toPortableString().compareTo(res2.getFullPath().toPortableString());
        }

    }

}
