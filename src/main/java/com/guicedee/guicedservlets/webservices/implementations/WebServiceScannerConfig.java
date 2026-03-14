package com.guicedee.guicedservlets.webservices.implementations;

import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuiceConfigurator;

public class WebServiceScannerConfig implements IGuiceConfigurator<WebServiceScannerConfig> {
	@Override
	public IGuiceConfig<?> configure(IGuiceConfig<?> config) {

		config.setAnnotationScanning(true);
		config.setMethodInfo(true);
		config.setClasspathScanning(true);
		config.setFieldInfo(true);

		return config;
	}

}
