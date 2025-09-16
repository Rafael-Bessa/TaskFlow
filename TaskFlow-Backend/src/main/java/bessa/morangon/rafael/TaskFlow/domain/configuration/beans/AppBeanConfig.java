package bessa.morangon.rafael.TaskFlow.domain.configuration.beans;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeanConfig {
    @Bean
    public ModelMapper getInstance(){
        return new ModelMapper();
    }


}
