# aq-demo
Example with hacked code to use custom Oracle objects with Advanced Queueing with Micronaut.

The base example using only strings as message types can be found here:

https://blogs.oracle.com/developers/post/enterprise-messaging-via-oracle-advanced-queuing-with-autonomous-db-micronaut

For the rest of the Oracle stuff follow the  mentioned above, 
I replaced the sys.aq$_jms_text_message used in the example with 
a custom type:

create type message_t as object 
(
ident  integer,
message varchar2 ( 512 )
);



Example for Spring which gave me some ideas can be found here:

https://blog.javaforge.net/post/30858904340/oracle-advanced-queuing-spring-custom-types

Some explanation on Oracle types and how to map them for use in java can be found here:

https://docs.oracle.com/cd/E11882_01/java.112/e16548/oraoot.htm#JJDBC28431

Basically, I replaced Micronaut's JMSListenerContainerFactory, JMSListenerContainer and DefaultSerializerDeserializer classes 
with a hacked version to make them work with Oracle AQ and custom types.

