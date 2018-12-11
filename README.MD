Olivia, in the name of my wife, is a gradle plugin to invoke solc and web3j to compile and wrapper .sol files.

****
solidity antlr4 gramma is referred from solidityj, https://github.com/solidityj/solidity-antlr4
solc, a.k.a. solidity compiler comes from Ethereum Solidity Org, https://github.com/ethereum/solidity/releases
web3j wrapper is from Web3j Org, https://github.com/web3j/web3j
Thanks to above for building a colorful blockchain world
****

This is current an MVP, in development and unstable, but can work. Test case not finished.

This plugin has NOT been published to any repositories, so that you have to clone and build it by your self and publish to repository.

Please be happy and free to let me hear your voice.

Use task 'olivia' after applying this plugin
```a
plugins {
    id 'me.lycrus.SolidityBuilder' version '1.0'
}
```

You can refer build.gradle in test resource. Here is an example first:

```
solidity {

    source "myPackage", "$projectDir/src/main/solidity/contracts",{
        include "**/*.sol"
    }
    source "otherPackage", "$projectDir/src/main/smartContracts/", "other/packages", {
        include "**/*.sol"
    }

    libraryRoot "$projectDir/src/main/solidity/libraries"

    library "$projectDir/src/main/solidity/custom_library/cusLib"

    remapping "testLib", "$projectDir/src/main/solidity/test_library/testLib", "testLib"

    remapping "openzeppelin-solidity", "$projectDir/src/main/solidity/node_modules/openzeppelin-solidity/", "openzeppelinSolidity"

    //version "provided"
    //version "native"
    version "0.4.25"
} 

```

Please read class SolidityBuilderExtension to get more info

Key points:

1. Olivia monitors sol files, but for normally usage, no one should modify libraries and add/remove libraries frequently;
So that declare your SOURCE sol files by using 'source', declare your LIBRARY sol files by 'libraryRoot', 'library' and 'remapping'

2. Olivia have to deal solc remapping and java package name for .sol files and smart contracts.
You can manually give a rule for solc remapping and java package name.


for example 
```
remapping "testLib", "$projectDir/src/main/solidity/test_library/testLib", "testLib"
```
this means you add a remapping testLib/=$projectDir/src/main/solidity/test_library/testLib/ to solc
and all files under $projectDir/src/main/solidity/test_library/testLib/ will be treated under java package testLib
for web3j's wrapper
```
source "myPackage", "$projectDir/src/main/solidity/contracts",{
        include "**/*.sol"
    }
````
this is will tell gradle to monitor these files as Input for incremental build and then is equivalent to 
```
remapping "myPackage", "$projectDir/src/main/solidity/contracts", "myPackage"
```
and you can add a clouse to config the fileTree

3. You can give 3 options : "provided", "native", "X.X.X" for choose solc compile.
"provided" : use solc packaged with Olivia, current provided version is 0.4.25
"native" : use your native solc, you must install solc and add it to $path
"X.X.X" : download specific version from solidity website and use it. Currently not support MacOs and I will add this feature.

4. Olivia has a template project structure, please see 'pluginTest' project in /src/test/resources. That's easy for you to understand and you can test Olivia.