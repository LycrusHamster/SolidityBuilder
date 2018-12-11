package me.lycrus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@NoArgsConstructor
@Getter
@Setter
public class SmartContract {
    public Sol file;
    public String contractName;
    public Path jsonFile;

    @Override
    public String toString() {
        return "SmartContract{" +
                "file=" + file +
                ", contractName='" + contractName + '\'' +
                ", jsonFile=" + jsonFile +
                '}';
    }
}
