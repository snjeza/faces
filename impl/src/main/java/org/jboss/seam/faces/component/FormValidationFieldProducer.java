package org.jboss.seam.faces.component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.seam.faces.event.qualifier.After;
import org.jboss.seam.faces.event.qualifier.Before;
import org.jboss.seam.faces.validation.InputField;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com>Lincoln Baxter, III</a>
 * 
 */
@RequestScoped
public class FormValidationFieldProducer
{
   Logger log = Logger.getLogger(FormValidationFieldProducer.class);

   @Inject
   FacesContext context;

   UIForm form = null;
   UIValidateForm validator = null;
   private Map<String, UIInput> components;

   public void interceptComponentTree(@Observes @Before final UIValidateForm event)
   {
      validator = event;
      form = validator.locateForm();
      locateAliasedComponents(event);
      event.setComponents(components);
   }

   public void cleanupComponentTree(@Observes @After final UIValidateForm event)
   {
      components = new HashMap<String, UIInput>();
   }

   @Produces
   @Dependent
   @InputField
   public Object getInputFieldValue(final InjectionPoint ip)
   {
      Object result = null;

      if (isInitialized())
      {
         String id = getFieldId(ip);
         UIInput component = findComponent(id, id);
         components.put(id, component);

         if (component.isLocalValueSet())
         {
            result = component.getValue();
         }
         else
         {
            Converter converter = component.getConverter();
            if (converter != null)
            {
               result = converter.getAsObject(context, component, (String) component.getSubmittedValue());
            }
            else
            {
               result = component.getSubmittedValue();
            }
         }

      }

      return result;
   }

   private boolean isInitialized()
   {
      return form != null;
   }

   private String getFieldId(final InjectionPoint ip)
   {
      String parameterName = ip.getAnnotated().getAnnotation(InputField.class).value();
      if ("".equals(parameterName))
      {
         parameterName = ip.getMember().getName();
      }
      return parameterName;
   }

   public void locateAliasedComponents(final UIValidateForm validator)
   {
      components = new HashMap<String, UIInput>();
      String fields = validator.getFields();
      if ((fields != null) && !"".equals(fields.trim()))
      {
         List<String> clientFieldIds = Arrays.asList(fields.split("\\s+"));
         for (String field : clientFieldIds)
         {
            List<String> mapping = Arrays.asList(field.split("\\s*=\\s*"));
            String aliasFieldName = mapping.get(0);

            String clientInputId = aliasFieldName;

            if (mapping.size() > 1)
            {
               clientInputId = mapping.get(1);
            }

            UIInput component = findComponent(aliasFieldName, clientInputId);
            components.put(aliasFieldName, component);
         }
      }
   }

   private UIInput findComponent(final String alias, final String clientId)
   {
      UIComponent comp = null;
      if (!components.containsKey(clientId))
      {
         comp = form.findComponent(clientId);
         if (comp == null)
         {
            throw new IllegalArgumentException("org.jboss.seam.component.UIValidateForm-- Could not locate component ["
                     + form.getClientId() + ":" + alias + "]");
         }
         else if (!(comp instanceof UIInput))
         {
            throw new IllegalArgumentException("org.jboss.seam.component.UIValidateForm-- Selected component ["
                     + form.getClientId() + ":" + alias + "] must be a UIInput component, was ["
                     + comp.getClass().getName() + "]");
         }
      }
      else
      {
         comp = components.get(clientId);
      }
      return (UIInput) comp;
   }

}
