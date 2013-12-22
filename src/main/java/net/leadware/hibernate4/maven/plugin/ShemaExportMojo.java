/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.leadware.hibernate4.maven.plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * Mojo executant le goal hbm2ddl 
 * @author <a href="mailto:jetune@leadware.net">Jean-Jacques ETUNE NGI</a>
 * @since 22 dec. 2013 - 12:48:12
 */
@SuppressWarnings("deprecation")
@Mojo(name = "hbm2ddl", 
	  defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES, 
	  threadSafe = true, 
	  requiresProject = true, 
	  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ShemaExportMojo extends AbstractMojo {
    
	/**
	 * Projet Maven en cours
	 */
	@Component
	private MavenProject project;
	
	/**
	 * Nom de l'unite de persistence
	 */
	@Parameter
	private String unitName;
	
	/**
	 * Delimiteur
	 */
	@Parameter(defaultValue = ";")
	private String delimiter;
	
	/**
	 * Fichier d'exportation des scripts de nettoyage
	 */
	@Parameter(defaultValue = "${project.build.directory}/hibernate/drop.sql")
	private String dropOutputFile;

	/**
	 * Fichier d'exportation des scripts de creation
	 */
	@Parameter(defaultValue = "${project.build.directory}/hibernate/create.sql")
	private String createOutputFile;

	/**
	 * Fichier d'exportation des scripts de mise a jour
	 */
	@Parameter
	private String updateOutputFile;
	
	/**
	 * Scripts additionnels
	 */
	@Parameter(property = "extendedScripts")
	private ExtendedScripts extendedScripts;
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
    	
    	// Un log
    	getLog().info("Exportation de l'Unite de persistence: " + unitName + ".");
    	
    	// Initialisation du repertoire de sortie
    	initOutputDirectory();

		// Fichier de drop
		File dropFile = new File(dropOutputFile.trim());

		// Fichier de drop
		File createFile = new File(createOutputFile.trim());

		// Fichier de drop
		File updateFile = null;
		
    	// Obtention du Thread courant
    	final Thread currentThread = Thread.currentThread();
    	
    	// Obtention du stream de sortie
    	final PrintStream oldOut = System.out;
    	
    	// Obtention du classloader du thread en cours
    	final ClassLoader oldClassLoader = currentThread.getContextClassLoader();
    	
    	try {
			
    		// Positionnement de la sortie par defaut
    		System.setOut(new PrintStream(new ByteArrayOutputStream()));
    		
    		// Positionnement du classloader avec ajout des chemins de classe du projet maven sous-jacent
    		currentThread.setContextClassLoader(buildClassLoader(oldClassLoader));
    		
    		// Configuration EJB3
    		final Ejb3Configuration jpaConfiguration = new Ejb3Configuration().configure(unitName, null);
    		
    		// Configuration Hibernate
    		Configuration configuration = jpaConfiguration.getHibernateConfiguration();
    		
    		// Exporteur de schema
    		SchemaExport exporter = new SchemaExport(configuration);
    		
    		// Positionnement du delimiteur
    		exporter.setDelimiter(delimiter);
    		
    		// Positionnement du fichier de sortie en drop
    		exporter.setOutputFile(dropFile.getAbsolutePath());
    		
    		// Exportation des scripts drop
    		exporter.execute(true, false, true, false);
    		
    		// Positionnement du fichier de sortie en create
    		exporter.setOutputFile(createFile.getAbsolutePath());
    		
    		// Exportation des scripts drop
    		exporter.execute(true, false, false, true);
    		
    		// Si le chemin des scripts de mise a jour est positionne
    		if(updateOutputFile != null && !updateOutputFile.trim().isEmpty()) {
    			
        		// Modificateur de schema
        		SchemaUpdate updater = new SchemaUpdate(configuration);

        		// Fichier de drop
        		updateFile = new File(updateOutputFile.trim());
        		
        		// Positionnement du fichier de sortie en create
        		updater.setOutputFile(updateFile.getAbsolutePath());
        		
        		// Exportation des scripts drop
        		updater.execute(true, true);
    		}
    		
    		// Si il ya des cripts additionnels
    		if(extendedScripts != null) {
    			
    			// Parcours de la liste des scripts de creation
    			for (String script : extendedScripts.getCreateScripts()) {
    				
    				// Ajout du script dans le fichier
    				FileUtils.fileAppend(createFile.getAbsolutePath(), "\n\t" + script);
    			}
    			
    			// Parcours de la liste des scripts de suppression
    			for (String script : extendedScripts.getDropScripts()) {
    				
    				// Ajout du script dans le fichier
    				FileUtils.fileAppend(dropFile.getAbsolutePath(), "\n\t" + script);
    			}
    			
    			// Si le chemin des scripts de mise a jour est positionne
    			if(updateOutputFile != null && !updateOutputFile.trim().isEmpty()) {

        			// Parcours de la liste des scripts de mise a jour
        			for (String script : extendedScripts.getUpdateScripts()) {
        				
        				// Ajout du script dans le fichier
        				FileUtils.fileAppend(updateFile.getAbsolutePath(), "\n\t" + script);
        			}
        			
    			}
    		}
    		
		} catch (Exception e) {
			
			// On relance
			throw new MojoExecutionException(e.getMessage(), e);
			
		} finally {
			
			// On repositionne la sortie standard
			System.setOut(oldOut);
			
			// On repositionne le classloader
			currentThread.setContextClassLoader(oldClassLoader);
		}
    }
    
    /**
     * Methode d'initialisation du repertoire de sortie
     */
    private void initOutputDirectory() {
    	
    	// Fichier create
    	File createDir = new File(createOutputFile.trim()).getParentFile();
    	
    	// Si le repertoire n'existe pas
    	if(!createDir.exists()) createDir.mkdirs();
    	
    	// Fichier drop
    	File dropDir = new File(dropOutputFile.trim()).getParentFile();
    	
    	// Si le repertoire n'existe pas
    	if(!dropDir.exists()) dropDir.mkdirs();
    	
    	// Si le fichier de mise a jour est renseigne
    	if(updateOutputFile != null && !updateOutputFile.trim().isEmpty()) {

        	// Fichier drop
        	File updateDir = new File(updateOutputFile.trim()).getParentFile();
        	
        	// Si le repertoire n'existe pas
        	if(!updateDir.exists()) updateDir.mkdirs();
        	
    	}
    }
    
    @SuppressWarnings("unchecked")
	private ClassLoader buildClassLoader(final ClassLoader delegate) {
    	
    	try {
			
    		// Liste des chemins de classes
    		final List<String> classpathElements = new ArrayList<String>();
    		
    		// Ajout des chemins de classe de compilation
    		classpathElements.addAll(project.getCompileClasspathElements());
    		
    		// Ajout des chemins de classe d'execution
    		classpathElements.addAll(project.getRuntimeClasspathElements());

    		// Ajout des chemins de classe de build
    		classpathElements.add(project.getBuild().getOutputDirectory());
    		
    		// Tableau des URL du classpath
    		final URL urls[] = new URL[classpathElements.size()];
    		
    		// Parcours de la liste des chemins de classes
    		for (int i = 0; i < classpathElements.size(); ++i) {
    			
    			// Ajout de l'URL
    			urls[i] = new File(classpathElements.get(i)).toURI().toURL();
    		}
    		
    		// Construction du ClassLoader
    		ClassLoader buildedClassloader = new URLClassLoader(urls, delegate);
    		
    		// On retourne le Classloader
    		return buildedClassloader;
    		
		} catch (Exception e) {
			
			// Un Log
			getLog().debug("Erreur lors de la construction de ClassLoader Maven pour le Plugin");
			 
			// On retourne le ClassLoader actuel
			return this.getClass().getClassLoader();
		}
    }
}
