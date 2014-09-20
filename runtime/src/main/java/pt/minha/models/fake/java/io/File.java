/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2014, Universidade do Minho.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package pt.minha.models.fake.java.io;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import pt.minha.models.local.lang.SimulationThread;

public class File {
	private final String path;
	private final java.io.File impl;
	private final String hostName;
	
	public static final char separatorChar     = java.io.File.separatorChar;
	public static final String separator       = java.io.File.separator;
	public static final char pathSeparatorChar = java.io.File.pathSeparatorChar;
	public static final String pathSeparator   = java.io.File.pathSeparator;
	
	public File(String path) {
		this(null, path);
	}
	
	public File(File parent, String path) {
		if (parent != null)
			path = parent.getPath() + File.separator + path;
		this.hostName = SimulationThread.currentSimulationThread().getHost().getName();
		this.createTree();
		this.path = this.removeNonVirtPath(path);
		this.impl = new java.io.File(this.hostName + File.separator + this.path);
	}
	
	private String removeNonVirtPath(String path){
		if(!path.contains(this.hostName)) return path;
		
		return path.split(this.hostName)[1];
	}
	
	private File(java.io.File f){
		this.hostName = SimulationThread.currentSimulationThread().getHost().getName();
		this.impl = f;
		this.path = this.removeNonVirtPath(f.getPath());
	}
	
	java.io.File getImpl(){
		return this.impl;
	}
	
	public boolean createNewFile() throws IOException{
		return this.impl.createNewFile();
	}
	
	public void deleteOnExit(){
		this.impl.deleteOnExit();
	}
	
	public long length(){
		return this.impl.length();
	}
	
	/**
	 * Function responsible for directory tree creation
	 * @param dir - name of disk directory, in this case ip
	 */
	private void createTree(){
		java.io.File newDir = new java.io.File(this.hostName);
		if(!newDir.exists()) newDir.mkdir();
	}

	//TODO: Review
	public String getAbsolutePath() {
		return this.removeNonVirtPath(this.impl.getAbsolutePath());
	}

	//TODO: Review
	public String getPath(){
		return this.path;
	}
	
	//TODO: Review
	public String getCanonicalPath() throws IOException{
		return this.removeNonVirtPath(this.impl.getCanonicalPath());
	}
	
	//TODO: Review
	public String getParent(){
		String parent = this.impl.getParent();
		
		if(parent == null || parent.equals(this.hostName)) return null;
		
		return this.removeNonVirtPath(parent);
	}
	
	public boolean isFile() {
		return this.impl.isFile();
	}

	public boolean isDirectory() {
		return this.impl.isDirectory();
	}
	
	public boolean canWrite() {
		return this.impl.canWrite();
	}
	
	public boolean delete() {
		return this.impl.delete();
	}
	
	public String[] list() {
		return this.impl.list();
	}
	
	public boolean exists() {
		return this.impl.exists();
	}
	
	public boolean mkdir() {
		return this.impl.mkdir();
	}
	
    public boolean mkdirs() {
    	return this.impl.mkdirs();
    }
    
    public boolean renameTo(File dest){
    	return this.impl.renameTo(dest.getImpl());
    }
    
    public File[] listFiles(){
    	ArrayList<File> files = new ArrayList<File>();
    	
    	for(java.io.File f : this.impl.listFiles())
    		files.add(new File(f));
    	
    	return files.toArray(new File[files.size()]);
    }
    
    public String[] list(final FilenameFilter ff) {
    	return impl.list(new java.io.FilenameFilter() {
			public boolean accept(java.io.File dir, String name) {
				return ff.accept(new File(dir), name);
			}    		
    	});
    }
    
    public URI toURI() {
    	try {
			return new URI("file", this.impl.getAbsolutePath(), null);
		} catch (URISyntaxException e) {
			// shouldn't happen
			return null;
		}
    }
    
    public String toString() {
    	return getPath();
    }
}
