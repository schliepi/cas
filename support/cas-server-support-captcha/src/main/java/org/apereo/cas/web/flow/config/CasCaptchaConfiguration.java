package org.apereo.cas.web.flow.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.CaptchaActivationStrategy;
import org.apereo.cas.web.CaptchaValidator;
import org.apereo.cas.web.DefaultCaptchaActivationStrategy;
import org.apereo.cas.web.flow.CasCaptchaWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.InitializeCaptchaAction;
import org.apereo.cas.web.flow.ValidateCaptchaAction;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasCaptchaConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnProperty(prefix = "cas.google-recaptcha", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration(value = "CasCaptchaConfiguration", proxyBeanMethods = false)
public class CasCaptchaConfiguration {

    @ConditionalOnMissingBean(name = "captchaWebflowConfigurer")
    @Bean
    public CasWebflowConfigurer captchaWebflowConfigurer(
        final CasConfigurationProperties casProperties, final ConfigurableApplicationContext applicationContext,
        @Qualifier(CasWebflowConstants.BEAN_NAME_LOGIN_FLOW_DEFINITION_REGISTRY)
        final FlowDefinitionRegistry loginFlowDefinitionRegistry,
        @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_BUILDER_SERVICES)
        final FlowBuilderServices flowBuilderServices) {
        return new CasCaptchaWebflowConfigurer(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties);
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "captchaValidator")
    public CaptchaValidator captchaValidator(final CasConfigurationProperties casProperties) {
        return CaptchaValidator.getInstance(casProperties.getGoogleRecaptcha());
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "validateCaptchaAction")
    public Action validateCaptchaAction(
        @Qualifier("captchaActivationStrategy")
        final CaptchaActivationStrategy captchaActivationStrategy,
        @Qualifier("captchaValidator")
        final CaptchaValidator captchaValidator) {
        return new ValidateCaptchaAction(captchaValidator, captchaActivationStrategy);
    }

    @Bean
    @ConditionalOnMissingBean(name = "captchaActivationStrategy")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CaptchaActivationStrategy captchaActivationStrategy(@Qualifier(ServicesManager.BEAN_NAME)
                                                               final ServicesManager servicesManager) {
        return new DefaultCaptchaActivationStrategy(servicesManager);
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "initializeCaptchaAction")
    public Action initializeCaptchaAction(final CasConfigurationProperties casProperties,
                                          @Qualifier("captchaActivationStrategy")
                                          final CaptchaActivationStrategy captchaActivationStrategy) {
        return new InitializeCaptchaAction(captchaActivationStrategy,
            requestContext -> requestContext.getFlowScope().put("recaptchaLoginEnabled", casProperties.getGoogleRecaptcha().isEnabled()),
            casProperties.getGoogleRecaptcha());
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "captchaCasWebflowExecutionPlanConfigurer")
    public CasWebflowExecutionPlanConfigurer captchaCasWebflowExecutionPlanConfigurer(
        @Qualifier("captchaWebflowConfigurer")
        final CasWebflowConfigurer captchaWebflowConfigurer) {
        return plan -> plan.registerWebflowConfigurer(captchaWebflowConfigurer);
    }
}
