package org.apereo.cas.validation.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.RegisteredServiceAccessStrategyUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.validation.Cas10ProtocolValidationSpecification;
import org.apereo.cas.validation.Cas20ProtocolValidationSpecification;
import org.apereo.cas.validation.Cas20WithoutProxyingValidationSpecification;
import org.apereo.cas.validation.CasProtocolValidationSpecification;
import org.apereo.cas.validation.DefaultServiceTicketValidationAuthorizersExecutionPlan;
import org.apereo.cas.validation.RegisteredServiceRequiredHandlersServiceTicketValidationAuthorizer;
import org.apereo.cas.validation.RequestedAuthenticationContextValidator;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizer;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizerConfigurer;
import org.apereo.cas.validation.ServiceTicketValidationAuthorizersExecutionPlan;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Optional;

/**
 * This is {@link CasCoreValidationConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casCoreValidationConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasCoreValidationConfiguration {
    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CasProtocolValidationSpecification cas10ProtocolValidationSpecification() {
        return new Cas10ProtocolValidationSpecification(servicesManager.getObject());
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CasProtocolValidationSpecification cas20ProtocolValidationSpecification() {
        return new Cas20ProtocolValidationSpecification(servicesManager.getObject());
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CasProtocolValidationSpecification cas20WithoutProxyProtocolValidationSpecification() {
        return new Cas20WithoutProxyingValidationSpecification(servicesManager.getObject());
    }

    @Autowired
    @Bean
    @ConditionalOnMissingBean(name = "serviceValidationAuthorizers")
    public ServiceTicketValidationAuthorizersExecutionPlan serviceValidationAuthorizers(final List<ServiceTicketValidationAuthorizerConfigurer> configurers) {
        val plan = new DefaultServiceTicketValidationAuthorizersExecutionPlan();
        configurers.forEach(c -> {
            LOGGER.trace("Configuring service ticket validation authorizer execution plan [{}]", c.getName());
            c.configureAuthorizersExecutionPlan(plan);
        });
        return plan;
    }

    @Bean
    public ServiceTicketValidationAuthorizer requiredHandlersServiceTicketValidationAuthorizer() {
        return new RegisteredServiceRequiredHandlersServiceTicketValidationAuthorizer(this.servicesManager.getObject());
    }

    @Bean
    public ServiceTicketValidationAuthorizerConfigurer casCoreServiceTicketValidationAuthorizerConfigurer() {
        return plan -> plan.registerAuthorizer(requiredHandlersServiceTicketValidationAuthorizer());
    }

    @Bean
    @ConditionalOnMissingBean(name = "requestedContextValidator")
    public RequestedAuthenticationContextValidator requestedContextValidator() {
        return (assertion, request) -> {
            LOGGER.trace("Locating the primary authentication associated with this service request [{}]", assertion.getService());
            val service = servicesManager.getObject().findServiceBy(assertion.getService());
            RegisteredServiceAccessStrategyUtils.ensureServiceAccessIsAllowed(assertion.getService(), service);
            return Pair.of(Boolean.TRUE, Optional.empty());
        };
    }
}
