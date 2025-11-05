package mppb.microservices.common.infrastructure.env;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RuntimeEnv {

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public boolean shouldRequireAuthToken() {
        if(constainsProfile("prod")){
            return true;
        }
        return !constainsProfile("insecure");
    }

    public boolean shouldIncludeTrace() {
        return !constainsProfile("prod") || constainsProfile("trace");
    }

    private boolean constainsProfile(String profileName) {
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return false;
        }
        List<String> profiles= Arrays.asList(activeProfiles.split(","));
        return profiles.contains(profileName);
    }


}
