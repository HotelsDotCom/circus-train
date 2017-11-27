# Avro Schema Replication

##  Overview
Download avro schemas from the uri on the source table and upload it to a user specified 'base-url' on replication. A base-url
is a user specified url. 

Circus Train will place the '.avsc' file into a hidden '.schema' folder to prevent Hive from interpreting the .avsc file as a Hive 
data file in the situation that the table-location and the base-url specified by the user are equivalent.

The avro schema file is copied into the following location if the base-url is NOT equivalent to the replica table location: 
    '${base-url}/eventId/<avro-file.avsc>'.

The avro schema file is copied into the following location if the base-url is equivalent to the replica table location: 
    '${base-url}/eventId/.schema/<avro-file.avsc>'.
    
(Note that the eventId is generated by circus-train and the '.schema' folder is a hidden folder.)


## Configuration
The base-url can be set at either the global level, or on a per table replication basis. The base-url can also be set at 
the global level, then be overriden for certain replications.

#### Examples:

    #Global setting example:
    table-replications:
      -
        source-table:
          database-name: example
          table-name: table
        replica-table:
          database-name: my_database
          table-name: table_copy
          table-location: /example/url
      -    
        source-table:
          database-name: example
          table-name: table_two
        replica-table:
          database-name: my_database
          table-name: table_two_copy
          table-location: /example/url
        
    avro-serde-options:
      base-url: /example/url/
      #Schema url's for each table will be copied to /example/url/<eventId>/.schema/


    #Per table replication example:
    table-replications:
      -
        source-table:
          database-name: example
          table-name: table
        
        replica-table:
          database-name: my_database
          table-name: copied_table
          table-location: /example/url/
          transform-options:
            avro-serde-options:
                #avro schema file will be copied to /base/url/<eventId>/ 
                base-url: /base/url/
      -  
         source-table:
           database-name: foo
           table-name: baz
         
         replica-table:
           database-name: my_database
           table-name: baz_copy
           table-location: /foo/baz/copy/is/here
           transform-options:
             avro-serde-options:
                 #avro schema file will be copied to /bazs/new/url/<eventId>/ 
                 base-url: /bazs/new/url/
          
           
    #Global setting with override on specific table example:
    table-replications:
      -
        source-table:
          database-name: example
          table-name: table
        
        replica-table:
          table-name: copied_table
          table-location: /example/url/
          transform-options:
            avro-serde-options:
                #avro schema file will be copied to /overriden/url/<eventId>/ rather than /global/url/<eventId>/
                base-url: /overriden/url/
    
        avro-serde-options:
            base-url: /global/url



 
 