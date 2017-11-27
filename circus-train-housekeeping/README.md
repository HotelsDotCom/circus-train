# Circus Train Housekeeping

## Overview
A database-backed module that stores orphaned replica paths in a table for later clean up.

## Configuration
The circus train housekeeping module defaults to using the H2 Database Engine, however this module can be configured 
to use any flavour of SQL that is supported by JDBC, Spring Boot and Hibernate. Using a database which is not in memory
should be preferred when temporarily spinning up instances for jobs before tearing them down. This way the orphaned data 
will still be cleaned from S3, even if the cluster ceases to exist.

### Example Configuration:
    
    housekeeping:
      data-source: 
        #The package of your driver class 
        driver-class-name: com.mysql.cj.jdbc.Driver
        #JDBC URL for your Database
        url: jdbc:mysql://circus-train-housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/circus_train
        #Database Username
        username: bdp
        #Database Password
        password: Ch4ll3ng3

In order to connect to your SQL database, you must place a database connector jar that is compatible with your Database 
into $CIRCUS_TRAIN_HOME/lib . Alternatively you can set the environment variable CIRCUS_TRAIN_CLASSPATH to contain the path to the jar file(s).

### Password Encryption
Circus Train allows users to provide encrypted passwords in the config. An encrypted password can be generated by doing
the following: 
 
    java -cp $CIRCUS_TRAIN_HOME/lib/jasypt-1.9.2.jar  org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="Ch4ll3ng3" password=ct_password algorithm=PBEWithMD5AndDES
    
    ----ENVIRONMENT-----------------
    
    Runtime: Oracle Corporation OpenJDK 64-Bit Server VM 25.121-b13 
    
    
    ----ARGUMENTS-------------------
    
    algorithm: PBEWithMD5AndDES
    input: Ch4ll3ng3
    password: ct_password
    
    
    ----OUTPUT----------------------
    
    EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by

The 'input' is your database password. The 'password' is a password specified by you that can be used to decrypt the data.
The 'output' is your encrypted password. This encrypted password can then be used in the Circus Train yaml configuration

    housekeeping:
      data-source: 
        #The package of your driver class 
        driver-class-name: com.mysql.cj.jdbc.Driver
        #JDBC URL for your Database
        url: jdbc:mysql://circus-train-housekeeping.foo1baz123.us-east-1.rds.amazonaws.com:3306/circus_train
        #Database Username
        username: bdp
        #Encrypted Database Password
        password: ENC(EHL/foiBKY2Ucy3oYmxdkFiXzWnOu7by)
            
Finally if you are using a encrypted password, when you run Circus Train you must provide the --password= parameter, providing your decryption password as an argument.
In this example you would run Circus Train Housekeeping as follows:

    $CIRCUS_TRAIN_HOME/bin/housekeeping.sh --config=/home/hadoop/conf.yml --password=ct_password

or           

    $CIRCUS_TRAIN_HOME/bin/circus-train.sh --config=/home/hadoop/conf.yml --password=ct_password

to run a Circus Train job followed by housekeeping.

If no housekeeping configuration is provided, Circus Train will default to using an in memory H2 Database.