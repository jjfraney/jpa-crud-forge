package org.jjflyboy.forge.addon.crud;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class CrudToolGenerationContext
{
   private Project project;
   private JavaClassSource entity;
   private String targetPackageName;
   private String persistenceUnitName;

   public JavaClassSource getEntity()
   {
      return entity;
   }

   public void setEntity(JavaClassSource entity)
   {
      this.entity = entity;
   }

   public String getTargetPackageName()
   {
      return targetPackageName;
   }

   public void setTargetPackageName(String targetPackageName)
   {
      this.targetPackageName = targetPackageName;
   }

   public String getPersistenceUnitName()
   {
      return persistenceUnitName;
   }

   public void setPersistenceUnitName(String persistenceUnitName)
   {
      this.persistenceUnitName = persistenceUnitName;
   }

   public Project getProject()
   {
      return project;
   }

   public void setProject(Project project)
   {
      this.project = project;
   }
}