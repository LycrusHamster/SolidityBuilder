package me.lycrus;

import org.apache.commons.lang3.StringUtils;
import org.web3j.codegen.TruffleJsonFunctionWrapperGenerator;

import java.util.Iterator;
import java.util.Set;

public class WrapperInvoker {

    public void wrapper(Set<SmartContract> writedCompiledTruffleContract, SolidityBuilderExtension solidityBuilderExtension) {
        Iterator<SmartContract> it = writedCompiledTruffleContract.iterator();
        if (StringUtils.isEmpty(solidityBuilderExtension.defaultPackageName)) {
            throw new RuntimeException("defaultPackageName mustn't be null or empty");
        }

        while (it.hasNext()) {
            SmartContract file = it.next();
            //check if contract is in excluded list
            String contractName = file.contractName;
            if (contractName == null) {
                throw new RuntimeException("field/element 'contractName' is missing in truffle-contract json");
            }

            Set<String> excludedContracts = solidityBuilderExtension.getExcludedContracts();

            boolean excluded = false;
            for (String excludedContract : excludedContracts) {
                if (excludedContract.equals(contractName)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) {
                continue;
            }

            //prepare package
            String[] pkgCombines = new String[]{solidityBuilderExtension.generatedPackageName,file.file.getPackage()};

            boolean fisrt = true;
            String contractFullName = "";
            for (String e : pkgCombines) {
                if(StringUtils.isNoneEmpty(e)){
                    if(!fisrt){
                        contractFullName += ".";
                    }
                    contractFullName += e;
                    fisrt = false;
                }
            }

            if(StringUtils.isEmpty(contractFullName)){
                contractFullName = solidityBuilderExtension.defaultPackageName;
            }

            String[] args = new String[6];
            if (solidityBuilderExtension.isUseNativeJavaTypes()) {
                args[0] = "--javaTypes";
            } else {
                args[0] = "-solidityTypes";
            }

            args[1] = file.jsonFile.toString();
            args[2] = "-o";
            args[3] = solidityBuilderExtension.wrapperOutputDir.toString();
            args[4] = "-p";
            args[5] = contractFullName;
            /*try{
                System.out.println("Web3j codegen version : " + Version.getVersion());
            }catch (IOException e) {
                e.printStackTrace();
            }*/

            try {
                TruffleJsonFunctionWrapperGenerator.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
