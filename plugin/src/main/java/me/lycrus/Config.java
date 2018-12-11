package me.lycrus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Config {

    @JsonProperty
    public int optimizer;


    @JsonProperty
    public String solidityOutputDir;

    @JsonProperty
    public String version;
    @JsonProperty
    public String generatedPackageName;
    @JsonProperty
    public boolean overwriteWrapper;
    @JsonProperty
    public Set<String> excludedContracts;
    @JsonProperty
    public boolean useNativeJavaTypes;
    @JsonProperty
    public Map<String, String> remappings;
    @JsonProperty
    public String npm;
    @JsonProperty
    public String ethpm;
    @JsonProperty
    public String contractDir;

    private Config() {

    }

    public static Config create(SolidityBuilderExtension sbe) {
        Config config = new Config();
       /* config.optimizer = sbe.optimizer;
        config.version = sbe.version;
        config.solidityOutputDir = sbe.solidityOutputDir;
        config.defaultPackageName = sbe.defaultPackageName;
        config.overwriteWrapper = sbe.overwriteWrapper;
        config.excludedContracts = sbe.excludedContracts;//we don't need deep copy
        config.useNativeJavaTypes = sbe.useNativeJavaTypes;
        config.remappings = sbe.remappings;
        config.npm = sbe.npm;
        config.ethpm = sbe.ethpm;
        config.contractDir = sbe.contractDir;*/
        return config;
    }

    public static Config load(File parent) {
        ObjectMapper om = new ObjectMapper();
        Config config = null;
        try {
            config = om.readValue(new File(parent, "config.json"), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public void save(File parent) {
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValue(new File(parent, "config.json"), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toJsonString() {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        String value = null;
        try {
            value = om.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (optimizer != config.optimizer) return false;
        if (overwriteWrapper != config.overwriteWrapper) return false;
        if (useNativeJavaTypes != config.useNativeJavaTypes) return false;
        if (solidityOutputDir != null ? !solidityOutputDir.equals(config.solidityOutputDir) : config.solidityOutputDir != null)
            return false;
        if (version != null ? !version.equals(config.version) : config.version != null) return false;
        if (generatedPackageName != null ? !generatedPackageName.equals(config.generatedPackageName) : config.generatedPackageName != null)
            return false;
        if (excludedContracts != null ? !excludedContracts.equals(config.excludedContracts) : config.excludedContracts != null)
            return false;
        if (remappings != null ? !remappings.equals(config.remappings) : config.remappings != null) return false;
        if (npm != null ? !npm.equals(config.npm) : config.npm != null) return false;
        return ethpm != null ? ethpm.equals(config.ethpm) : config.ethpm == null;
    }

    @Override
    public int hashCode() {
        int result = optimizer;
        result = 31 * result + (solidityOutputDir != null ? solidityOutputDir.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (generatedPackageName != null ? generatedPackageName.hashCode() : 0);
        result = 31 * result + (overwriteWrapper ? 1 : 0);
        result = 31 * result + (excludedContracts != null ? excludedContracts.hashCode() : 0);
        result = 31 * result + (useNativeJavaTypes ? 1 : 0);
        result = 31 * result + (remappings != null ? remappings.hashCode() : 0);
        result = 31 * result + (npm != null ? npm.hashCode() : 0);
        result = 31 * result + (ethpm != null ? ethpm.hashCode() : 0);
        return result;
    }
}
