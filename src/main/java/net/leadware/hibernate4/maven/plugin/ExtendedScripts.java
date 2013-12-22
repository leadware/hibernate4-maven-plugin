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

import java.util.ArrayList;
import java.util.List;

/**
 * Classe representant un fichier a generer par le plugin
 * @author <a href="mailto:jetune@leadware.net">Jean-Jacques ETUNE NGI</a>
 * @since 22 dec. 2013 - 17:56:44
 */
public class ExtendedScripts {
	
	/**
	 * Scripts etendus pour la creation
	 */
	private List<String> createScripts = new ArrayList<String>();
	
	/**
	 * Scripts etendus pour la modification
	 */
	private List<String> updateScripts = new ArrayList<String>();

	/**
	 * Scripts etendus pour la suppression
	 */
	private List<String> dropScripts = new ArrayList<String>();
	
	/**
	 * Constructeur par defaut
	 */
	public ExtendedScripts() {}

	/**
	 * Methode d'obtention du champ "createScripts"
	 * @return champ "createScripts"
	 */
	public List<String> getCreateScripts() {
		// Renvoi de la valeur du champ
		return createScripts;
	}

	/**
	 * Methode de modification du champ "createScripts"
	 * @param createScripts champ createScripts a modifier
	 */
	public void setCreateScripts(List<String> create) {
		
		// Modification de la valeur du champ
		this.createScripts = create;

		// Si la liste est vide
		if(this.createScripts == null) this.createScripts = new ArrayList<String>();
	}

	/**
	 * Methode d'obtention du champ "updateScripts"
	 * @return champ "updateScripts"
	 */
	public List<String> getUpdateScripts() {
		// Renvoi de la valeur du champ
		return updateScripts;
	}

	/**
	 * Methode de modification du champ "updateScripts"
	 * @param updateScripts champ updateScripts a modifier
	 */
	public void setUpdateScripts(List<String> update) {
		// Modification de la valeur du champ
		this.updateScripts = update;

		// Si la liste est vide
		if(this.updateScripts == null) this.updateScripts = new ArrayList<String>();
	}

	/**
	 * Methode d'obtention du champ "dropScripts"
	 * @return champ "dropScripts"
	 */
	public List<String> getDropScripts() {
		// Renvoi de la valeur du champ
		return dropScripts;
	}

	/**
	 * Methode de modification du champ "dropScripts"
	 * @param dropScripts champ dropScripts a modifier
	 */
	public void setDropScripts(List<String> drop) {
		// Modification de la valeur du champ
		this.dropScripts = drop;
		
		// Si la liste est vide
		if(this.dropScripts == null) this.dropScripts = new ArrayList<String>();
	}
	
}
