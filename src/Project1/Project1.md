Assignment 1

Deadlines

1st Assignment - 11th April (online - code + report/form)

URL for the submission form: https://forms.gle/74AVpoPwFjXAqiwu8

The development of the project should be performed using a private GitHub repository. When submitting the project, you will be asked to share the repository with the professors.

The slides used in the lectures for presenting the project are available in the following link: trab1-2526-en.pdf.

Objective

The objective of the assignment is to develop a distributed message delivery system, similar to email.

Each user will have an account in a domain, where they have their mailbox. A user always interacts with the servers of their domain to perform system operations. For example, to send messages to one or more users in the same or other domains, the user contacts the server of their domain, which will be responsible for forwarding the message to the servers of the recipients’ domains.

Architecture

The system will be composed of a set of servers, with each server associated with a domain. There will be three types of servers:

user server, for managing the domain’s users;
message server, for managing the messaging service;
(optionally) gateway server, which acts as a proxy and is responsible for forwarding operations to the domain’s servers.
In the first assignment, there is only one user server, one message server and optionally one gateway server in each domain. The following figure provides a high-level overview of the application architecture.



Each domain has an associated set of users. User management is done independently by each domain, i.e., each domain maintains only the list of its own users.

User management will be performed by the Users service, using the following operations:

creating a user;
obtaining the information associated with an existing user;
modifying the information associated with a user;
removing a user;
searching for users.
Each domain manages the messaging service for the users of that domain. Thus, the service maintains a mailbox associated with each of its users. A user’s mailbox contains the messages received by that user that have not yet been deleted. Additionally, the service is responsible for forwarding messages sent by the users of that domain.

Message management will be performed by the Messages service, using the following operations:

send a message to one or several users - to send a message, a user must contact the server where they have a mailbox; this server will forward the message to servers of other domains, if necessary;
obtain the list of message identifiers in a user’s mailbox, optionally filtering those messages according to a provided search string;
obtain a message from the mailbox;
delete a message from the mailbox;
delete a message sent by a user - to perform this operation, a user must contact the server where they have a mailbox; this server will forward the request to servers of other domains, if necessary.
Each domain may also have a gateway server, responsible for forwarding operations performed by clients to the domain’s servers. This server simultaneously implement the Users and Messages service interfaces. The server receives only REST requests but can interact with a REST or GRPC server.

Service Interfaces

The domain servers of the system will be developed using REST technology (JAX-RS) and (optionally) through GRPC.

To ensure interoperability with the test suite to be provided, the services must be implemented respecting pre-defined programming interfaces and the expected effects of their operations (including results and exceptions).

The interface code is available at the following link. The code includes documentation on how operations should be invoked and what the result of their invocation should be (including errors to be sent).

The zip contains a Maven project, which we suggest you use as a base for the assignment.

Note: The interfaces may only be modified by introducing new operations, or through optional parameters in pre-existing operations.

Note: The defined interfaces, with requests being made over unencrypted channels, are not secure. Security aspects will be addressed in the second assignment.

Machine Names and Auto-configuration

The test system will assign a name of the form server.domain to the docker instance where the servers will be launched. Thus, all servers in a domain will have the same suffix, indicating the domain they are associated with (i.e., if a machine’s hostname is srv.fct, the server is responsible for the fct domain).

It should be possible to discover the URL of all services: users, messages and gateway. To do this, they must implement a discovery mechanism based on periodic announcements (by the servers) and IP multicast communication.

This mechanism will also be the means used by the test suite to discover the servers instances.

The discovery protocol consists of periodically sending to a pre-agreed IP multicast address and port, a message containing a string with the following format:

<service-name>@>domain-name><tab><server-uri>
The <server-uri> must begin with http: or grpc: to indicate, respectively, that it is a REST or GRPC server. For example, the REST users service at domain fct, running in a machine named users0.fct will send the following message: Users@fct<tab>http://users0.fct/rest. Services names are: Users, Messages and Gateway.

Solution Requirements

The focus of this first assignment will be on Remote Invocation technologies and Distribution.

The solution does not need to tolerate component failures. The only failures to consider are (temporary) communication failures. (There is, therefore, no need to introduce component replication)

Compatibility with pre-defined interfaces and operations must be observed. However, it may be necessary to add more operations to meet some of the solution requirements.

Minimum Requirements (max: 8 points)

REST API - Functional REST message, users and gateway servers, with all messaging operations taking effect only on the local domain mailboxes;
Concurrency control - REST servers work correctly when multiple clients make requests concurrently.
Base Requirements (max: 13 points)

Auto-configuration - Multicast discovery of the users server works correctly;
Full functionality - Functional REST server, with all operations executing correctly when communication failures do not exceed 10 seconds.

Non-existing destinations - If a message is sent to a user that does not exist, the system places in the sender’s mailbox a notification message, which has the content of the original message with the id modified to: “mid.user” and subject to: “FAILED TO SEND mid TO user: UNKNOWN USER”, with mid and user being replaced by the original message id and the address of the user for whom the delivery failed.
Bonus Elements (max: 20 points)

Long-duration failure handling (max: 2 points) - The system must work correctly when communication failures can be long.

Send failure handling (max: 1 point) - If unable to send a message for 90 seconds (configurable via -extralongfault), the system places in the sender’s mailbox a notification message, which has the content of the original message with the id modified to: “mid.user” and subject to: “FAILED TO SEND mid TO user: TIMEOUT”, with mid and user being replaced by the message id and the address of the user for whom the delivery failed. After this, the server gives up sending the message.

Tests: 10g.

GRPC Servers (max: 2 points) - The system works with GRPC servers only;

Interoperable Servers (max: 4 points; alternative to GRPC Servers) - The system works with REST and GRPC servers in the same system;

Note: Bonus elements will not be fully considered if the base requirements are not satisfactorily met.

Depreciation Factors

The submitted code should follow good programming practices. Unnecessary code repetition, inclusion of magic constants, use of inadequate data structures, etc., may incur a penalty. (max: 2 points)

Lack of robustness and erratic behavior of the solution are grounds for penalty. (max: variable)

The solution should account for temporary communication channel failures.

Execution

The assignment can be done in groups of 1 or 2 students. Students in the same group do not need to be in the same practical class, although this is strongly recommended.

Evaluation

The assignment evaluation will take into account the following criteria:

Developed features and their conformity with the specification, based on the results of the automatic test suite;
Quality of the solution;
Quality of the developed code.
The student’s final grade is individual and will be less than or equal to the assignment grade, depending on the results obtained in the individual discussion, to be held in the 1st practical test.

Test Suite

The test suite intended to verify the conformity of the solution with the specification is available at Tester.

Development Environment

All provided support material assumes development in a Linux environment with Java 17. Validation of the assignment through the automatic test suite will use Docker technology.

Change History

3-Mar-2026: Initial version of the project description, API and Tester.