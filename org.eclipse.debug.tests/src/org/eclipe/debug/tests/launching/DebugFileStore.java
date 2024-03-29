/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipe.debug.tests.launching;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * Implementation of an in memory file store to test launch configurations on EFS
 */
public class DebugFileStore extends FileStore {
	
	/**
	 * Output steam for writing a file
	 */
	class DebugOutputStream extends  ByteArrayOutputStream {
		
		/* (non-Javadoc)
		 * @see java.io.ByteArrayOutputStream#close()
		 */
		public void close() throws IOException {
			super.close();
			DebugFileSystem.getDefault().setContents(toURI(), toByteArray());
		}
		
	}
	
	private URI uri;
	
	public DebugFileStore(URI id) {
		uri = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#childNames(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		URI[] uris = DebugFileSystem.getDefault().getFileURIs();
		List children = new ArrayList();
		IPath me = getPath();
		for (int i = 0; i < uris.length; i++) {
			URI id = uris[i];
			Path path = new Path(id.getPath());
			if (path.segmentCount() > 0) {
				if (path.removeLastSegments(1).equals(me)) {
					children.add(path.lastSegment());
				}
			}
		}
		return (String[]) children.toArray(new String[children.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#fetchInfo(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		byte[] contents = DebugFileSystem.getDefault().getContents(toURI());
		FileInfo info = new FileInfo();
		info.setName(getName());
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		if (contents == null) {
			info.setExists(false);
			info.setLength(0L);
		} else {
			info.setExists(true);
			info.setLength(contents.length);
			info.setDirectory(contents == DebugFileSystem.DIRECTORY_BYTES);
			if (info.isDirectory()) {
				info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, true);
			}
		}
		return info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#getChild(java.lang.String)
	 */
	public IFileStore getChild(String name) {
		try {
			return new DebugFileStore(new URI(getFileSystem().getScheme(), getPath().append(name).toString(), null));
		} catch (URISyntaxException e) {
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#getName()
	 */
	public String getName() {
		IPath path = getPath();
		if (path.segmentCount() > 0) {
			return path.lastSegment();
		}
		return "";
	}

	/**
	 * @return
	 */
	private IPath getPath() {
		URI me = toURI();
		IPath path = new Path(me.getPath());
		return path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#getParent()
	 */
	public IFileStore getParent() {
		IPath path = getPath();
		if (path.segmentCount() > 0) {
			try {
				return new DebugFileStore(new URI(getFileSystem().getScheme(), path.removeLastSegments(1).toString(), null));
			} catch (URISyntaxException e) {
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#openInputStream(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		byte[] contents = DebugFileSystem.getDefault().getContents(toURI());
		if (contents != null) {
			return new ByteArrayInputStream(contents);
		}
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", 
				"File does not exist: " + toURI()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#openOutputStream(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		return new DebugOutputStream();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#mkdir(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		IFileInfo info = fetchInfo();
		if (info.exists()) {
			if (!info.isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", 
						"mkdir failed - file already exists with name: " + toURI()));
			}
		} else {
			IFileStore parent = getParent();
			if (parent.fetchInfo().exists()) {
				DebugFileSystem.getDefault().setContents(toURI(), DebugFileSystem.DIRECTORY_BYTES);
			} else {
				if ((options & EFS.SHALLOW) > 0) {
					throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.jdt.debug.tests", 
							"mkdir failed - parent does not exist: " + toURI()));
				} else {
					parent.mkdir(EFS.NONE, null);
				}
			}
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#toURI()
	 */
	public URI toURI() {
		return uri;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#delete(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		DebugFileSystem.getDefault().delete(toURI());
	}
}
