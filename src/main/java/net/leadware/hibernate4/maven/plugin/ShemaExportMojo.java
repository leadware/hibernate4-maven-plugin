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

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.spi.PersistenceUnitTransactionType;

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
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * Mojo executant le goal hbm2ddl 
 * @author <a href="mailto:jetune@leadware.net">Jean-Jacques ETUNE NGI</a>
 * @since 22 dec. 2013 - 12:48:12
 */
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
	@Parameter(defaultValue = "")
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
	@Parameter(defaultValue = "")
	private String updateOutputFile;
	
	/**
	 * Dialecte de generation
	 */
	@Parameter(defaultValue = "")
	private String dialect;
	
	/**
	 * Nouveau Mapping ID
	 */
	@Parameter(defaultValue = "true")
	private String newGeneratorMappings;
	
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
    		// System.setOut(new PrintStream(new ByteArrayOutputStream()));
    		
    		// Positionnement du classloader avec ajout des chemins de classe du projet maven sous-jacent
    		// currentThread.setContextClassLoader(buildClassLoader(oldClassLoader));
    		
    		// Persistence XML Parser
    		PersistenceXmlParser persistenceXmlParser = new PersistenceXmlParser(new ClassLoaderServiceImpl(oldClassLoader), PersistenceUnitTransactionType.RESOURCE_LOCAL);
    		
    		// Parsing des fichier de persistence du classpath
    		List<ParsedPersistenceXmlDescriptor> persistenceUnits = persistenceXmlParser.doResolve(new HashMap<String, String>());
    		
    		// Configuration Hibernate
    		Configuration configuration = new Configuration();
    		
    		// Parcours des descripteurs
    		for (ParsedPersistenceXmlDescriptor persistenceUnit : persistenceUnits) {
    			
				System.out.println(persistenceUnit.getName());
				
    			// Si le nom n'est pas celui attendu
    			if(persistenceUnit.getName() == null || !persistenceUnit.getName().trim().equalsIgnoreCase(unitName.trim())) continue;
    			
    			// Obtention de la liste des noms de classe managees
    			List<String> managedClassNames = persistenceUnit.getManagedClassNames();
    			
    			// Si la liste est nulle
    			if(managedClassNames == null) managedClassNames = new ArrayList<String>();
    			
    			// Paracours des classes managees
    			for (String managedClassName : managedClassNames) {
					
    				System.out.println(managedClassName);
    				
    				// Ajout de la classe dans la configuration
    				configuration.addAnnotatedClass(Class.forName(managedClassName));
				}
			}
    		
    		// Si le dialect a ete precise dans la configuration du plugin
    		if(dialect != null && !dialect.trim().isEmpty()) configuration.setProperty("hibernate.dialect", dialect.trim());

    		// Si le newGeneratorMappings a ete precise dans la configuration du plugin
    		if(newGeneratorMappings != null && !newGeneratorMappings.trim().isEmpty()) configuration.setProperty("hibernate.id.new_generator_mappings", newGeneratorMappings.trim());
    		
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
    				
    				// Tentative de construction d'un File sur le la chaine script
    				File scriptFile = new File(script);
    				
    				// Si l'objet existe et est un fichier
    				if(scriptFile.exists() && scriptFile.isFile()) {
    					
    					// Ajout de son contenu dans le fichier de script en cours
    					FileUtils.fileAppend(createFile.getAbsolutePath(), "\n\n" + FileUtils.fileRead(scriptFile));
    					
    				} else {

        				// Ajout du script dans le fichier
        				FileUtils.fileAppend(createFile.getAbsolutePath(), "\n\t" + script);
    				}
    			}
    			
    			// Parcours de la liste des scripts de suppression
    			for (String script : extendedScripts.getDropScripts()) {
    				
    				// Tentative de construction d'un File sur le la chaine script
    				File scriptFile = new File(script);
    				
    				// Si l'objet existe et est un fichier
    				if(scriptFile.exists() && scriptFile.isFile()) {
    					
    					// Ajout de son contenu dans le fichier de script en cours
    					FileUtils.fileAppend(dropFile.getAbsolutePath(), "\n\n" + FileUtils.fileRead(scriptFile));
    					
    				} else {
    					
    					// Ajout du script dans le fichier
        				FileUtils.fileAppend(dropFile.getAbsolutePath(), "\n\t" + script);
    				}
    			}
    			
    			// Si le chemin des scripts de mise a jour est positionne
    			if(updateOutputFile != null && !updateOutputFile.trim().isEmpty()) {

        			// Parcours de la liste des scripts de mise a jour
        			for (String script : extendedScripts.getUpdateScripts()) {

        				// Tentative de construction d'un File sur le la chaine script
        				File scriptFile = new File(script);
        				
        				// Si l'objet existe et est un fichier
        				if(scriptFile.exists() && scriptFile.isFile()) {
        					
        					// Ajout de son contenu dans le fichier de script en cours
        					FileUtils.fileAppend(updateFile.getAbsolutePath(), "\n\n" + FileUtils.fileRead(scriptFile));
        					
        				} else {
        					
        					// Ajout du script dans le fichier
            				FileUtils.fileAppend(updateFile.getAbsolutePath(), "\n\t" + script);
        				}
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
    protected void initOutputDirectory() {
    	
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
	protected ClassLoader buildClassLoader(final ClassLoader delegate) {
    	
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
