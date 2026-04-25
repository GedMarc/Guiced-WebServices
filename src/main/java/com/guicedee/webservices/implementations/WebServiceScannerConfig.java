package com.guicedee.webservices.implementations;

import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuiceConfigurator;

/**
 * Configures classpath scanning to discover @WebService annotated classes.
 */
public class WebServiceScannerConfig implements IGuiceConfigurator<WebServiceScannerConfig> {
    @Override
    public IGuiceConfig<?> configure(IGuiceConfig<?> config) {
        config.setAnnotationScanning(true);
        config.setClasspathScanning(true);
        return config;
    }
}

