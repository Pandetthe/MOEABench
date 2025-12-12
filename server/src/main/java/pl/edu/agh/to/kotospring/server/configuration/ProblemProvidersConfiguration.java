package pl.edu.agh.to.kotospring.server.configuration;

import org.moeaframework.core.spi.RegisteredProblemProvider;
import org.moeaframework.problem.CEC2009.CEC2009ProblemProvider;
import org.moeaframework.problem.DTLZ.DTLZProblemProvider;
import org.moeaframework.problem.LSMOP.LSMOPProblemProvider;
import org.moeaframework.problem.LZ.LZProblemProvider;
import org.moeaframework.problem.MaF.MaFProblemProvider;
import org.moeaframework.problem.WFG.WFGProblemProvider;
import org.moeaframework.problem.ZCAT.ZCATProblemProvider;
import org.moeaframework.problem.ZDT.ZDTProblemProvider;
import org.moeaframework.problem.misc.MiscProblemProvider;
import org.moeaframework.problem.single.SingleObjectiveProblemProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProblemProvidersConfiguration {

    @Bean
    public RegisteredProblemProvider getLZProblemProvider() {
        return new LZProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getSingleObjectiveProblemProvider() {
        return new SingleObjectiveProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getDTLZProblemProvider() {
        return new DTLZProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getCEC2009ProblemProvider() {
        return new CEC2009ProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getMaFProblemProvider() {
        return new MaFProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getLSMOPProblemProvider() {
        return new LSMOPProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getWFGProblemProvider() {
        return new WFGProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getZDTProblemProvider() {
        return new ZDTProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider getZCATProblemProvider() {
        return new ZCATProblemProvider();
    }

    @Bean
    public RegisteredProblemProvider MiscProblemProvider() {
        return new MiscProblemProvider();
    }
}
