package edu.ncsu.las.model.collector.type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;


/**
 * ConfigurationTypeInterface so that multiple ConfigurationType classes can be implemented
 *  
 */
public interface ConfigurationTypeInterface {
	static Logger logger =Logger.getLogger(ConfigurationTypeInterface.class.getName());
	
	public String getLabel();
	
	public boolean isRequired();
	
	public ConfigurationTypeInterface getParentConfiguration();
	
	
	public String getDescription();
	
	public ConfigurationLocation getLocation();
	
	public boolean isOverrideable();
	
	public boolean isDerived();
	
	public boolean exposeToUserInterface();
	
	public String getDerivationPattern();
	
	public boolean isEncrypted();

	public SourceParameterType getType();
	
	public boolean isArray();
			
	public String getFullLabel();
	
	
	// access the configuration types by their full label.  This is lazily created when the first call is made.
	public static HashMap<String, ConfigurationTypeInterface> _configTypeByFullName = new HashMap<String, ConfigurationTypeInterface>();
	
	public static ConfigurationTypeInterface getConfigurationType(String fullLabel) {
		if (_configTypeByFullName.isEmpty()) {
			synchronized (ConfigurationTypeInterface.class) {
				Reflections reflections = new Reflections("edu.ncsu.las");    
				java.util.Set<Class<? extends ConfigurationTypeInterface>> classes = reflections.getSubTypesOf(ConfigurationTypeInterface.class);
				for (Class<? extends ConfigurationTypeInterface> c: classes) {
					if (c.getSuperclass().getName().equals("java.lang.Enum")) {
						try {
							Method method = c.getMethod("values");
							ConfigurationTypeInterface[] values = (ConfigurationTypeInterface[]) method.invoke(null);
							for (ConfigurationTypeInterface ct: values) {
								_configTypeByFullName.put(ct.getFullLabel(), ct);
							}
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							logger.log(Level.SEVERE, "Unable get enum values for class: "+c.getName());
						}
						
					}
				}

			}
		}
		return _configTypeByFullName.get(fullLabel);
	}
}
