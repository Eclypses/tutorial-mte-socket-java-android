

<img src="Eclypses.png" style="width:50%;margin-right:0;"/>

<div align="center" style="font-size:40pt; font-weight:900; font-family:arial; margin-top:300px; " >
Android Java Socket Tutorial</div>

<div align="center" style="font-size:28pt; font-family:arial; " >
MTE<sup>TM</sup> Implementation Tutorial </div>
<div align="center" style="font-size:15pt; font-family:arial; " >
Using MTE<sup>TM</sup> version 3.0.x</div>





[Introduction](#introduction)

[Socket Tutorial Client](#socket-tutorial-client)


<div style="page-break-after: always; break-after: page;"></div>

# Introduction

This tutorial is sending messages via a socket connection. This is only a sample, the MTE does NOT require the usage of sockets, you can use whatever communication protocol that is needed.

Note that you will need a server side tutorial too. This Android tutorial contains only the client side. Please pick a server side from one of the other programming languages.

This tutorial demonstrates how to use Mte Core, Mte MKE and Mte Fixed Length. Depending on what your needs are, these three different implementations can be used in the same application OR you can use any one of them. They are not dependent on each other and can run simultaneously in the same application if needed.

The SDK that you received from Eclypses may not include the MKE or MTE FLEN add-ons. If your SDK contains either the MKE or the Fixed Length add-ons, the name of the SDK will contain "-MKE" or "-FLEN". If these add-ons are not there and you need them please work with your sales associate. If there is no need, please just ignore the MKE and FLEN options.

Here is a short explanation of when to use each, but it is encouraged to either speak to a sales associate or read the dev guide if you have additional concerns or questions.

***MTE Core:*** This is the recommended version of the MTE to use. Unless payloads are large or sequencing is needed this is the recommended version of the MTE and the most secure.

***MTE MKE:*** This version of the MTE is recommended when payloads are very large, the MTE Core would, depending on the token byte size, be multiple times larger than the original payload. Because this uses the MTE technology on encryption keys and encrypts the payload, the payload is only enlarged minimally.

***MTE Fixed Length:*** This version of the MTE is very secure and is used when the resulting payload is desired to be the same size for every transmission. The Fixed Length add-on is mainly used when using the sequencing verifier with MTE. In order to skip dropped packets or handle asynchronous packets the sequencing verifier requires that all packets be a predictable size. If you do not wish to handle this with your application then the Fixed Length add-on is a great choice. This is ONLY an encoder change - the decoder that is used is the MTE Core decoder.

In this tutorial we are creating an MTE Encoder and an MTE Decoder in the server as well as the client because we are sending secured messages in both directions. This is only needed when there are secured messages being sent from both sides, the server as well as the client. If only one side of your application is sending secured messages, then the side that sends the secured messages should have an Encoder and the side receiving the messages needs only a Decoder.
In this tutorial we are creating a receiver and a sender in the server as well as the client because we are sending messages in both directions. This is only needed when there are messages being sent from both sides, the server as well as the client. If only one side of your application is sending messages, then the side that sends the messages should have an Encoder and the side receiving the messages needs only a Decoder.

These steps should be followed on the server side as well as on the client side of the program.

# Socket Tutorial Server
This tutorial does not include a server side. No matter which tutorial you pick for the server side, you have to make sure that the server will listen to your computer's actual IP address, not "localhost". Making the server listen to any interface will also work. The Android emulator runs in a different local network which connects to your actual computer using NAT.
# Socket Tutorial Client
**IMPORTANT:**
Please note the solution provided in this tutorial does NOT include the MTE library or supporting MTE library files. If you have NOT been provided an MTE library and supporting files, please contact Eclypses Inc. The solution will only work AFTER the MTE library and MTE library files have been incorporated (see below).
<br><ol>
<li>Open the project in Adroid Studio. Within the project root directory of "SocketTutorial-MTESolution" navigate to "mte/src/main/" (create the directories if they don't exist). Navigate to the "src/" directory in the MTE distribution archive and copy the "java/" directory including all of its subdirectories and files to the "mte/src/main/" directory of your project.</li>
<br>
<li>Navigate to the "app/src/main/" directory of the "SocketTutorial-MTESolution" project. Create a "jniLibs" directory. Then create subdirectories for the platforms you want to support:
<br>  • "arm64-v8a/" for the 64-bit ARM platform
<br>  • "armeabi-v7a/" for the 32-bit ARM platform
<br>  • "x86_64/" for the 64-bit Intel X86-64 platform
<br>  • "x86/" for the 32-bit Intel X86 platform<br>
Finally copy the "libmtejni.so" files from the lib directories of the according MTE distribution archives to the directories you just created.
</li>
<br>
<li>You will find all MTE related source in the "MyApplication" class This class, based on Android's "Application" class will ensure that MTE's configuration does not get destroyed and recreated every time the main activity gets pushed to the background. Depending on the variant of MTE (core, MKE, FLEN) you want to use, you will have to uncomment/comment the according lines in MyApplication.java - please note that you will have to change the source code in 2 places: the declaration and creation for the encoder and decoder pair.</li>
<br>
<li>"boolean setupMTE()" contains all the code necessary described below to get MTE up and running. "setupMTE()" is called from "boolean init(SetupParams)" which is called from the application's activity. The application's activity passes the SetupParams which contain necessary information for the communication itself (server ip and port). Al list of 7 easy to follow steps in setupMTE() will show you how to implement MTE.</li>
<br>
<li>To ensure the MTE library is licensed correctly run the license check and also run the DRBG's self test. The LicenseCompanyName, and LicenseKey below should be replaced with your company’s MTE license information. If a demo or trial version is being used any value can be passed into those fields and it will work.</li>
<br>
<li>For ease of implementation we are using the default (parameterless) constructors. These constructors will use the available buildtime options or a reasonable set of default options in case you are using a MTE library which incorporates runtime options.</li>
<br>
<li>Create MTE Decoder Instance and MTE Encoder Instances as shown in "setupMTE()"</li>
<br>
<li>The values for entropy, nonce and identifier values:</li>
These values should be treated like encryption keys. For demonstration purposes in the tutorial these are simply defined as constants which you can alter if you wish. In a production environment these values should be protected and not available to outside sources. 

For the entropy, we have to determine the size of the allowed entropy value based on the drbg we have selected. A code sample below is included to demonstrate how to get these values. For the trial library, both functions will return zero which means that the entropy string has be empty!

```java
MteDrbg drbg = encoder.getDrbg();
int entropyMinBytes = MteBase.getDrbgsEntropyMinBytes(drbg);
long entropyMaxBytes = MteBase.getDrbgsEntropyMaxBytes(drbg);
```

<li> After having created encoder and decoder and having created a useful demonstration entropy, instantiate them and try to cleanup secret values like entropy, nonce and identifier as quickly as possible.</li><br>
<li>Finally, we need to add the MTE calls to encode and decode the messages that we are sending and receiving from the other side. (Ensure on the server side the Encoder is called to encode the outgoing text, then the Decoder is called to decode the incoming response.)</li>
<br>
<li>In MyApplication.java you will find 2 functions, "byte[] encodeData(byte[])" and "byte[] decodeData(byte[])" which are called by the application's activity in order to encode outgoing data and decode incoming data.</li>
<br><br>
</ol>

***The Client side of the MTE Sockets Tutorial should now be ready for use on your device.<br><br>Please note that this client tutorial requires at least Android 7 (API24) in order to build and run successfully.***


<div style="page-break-after: always; break-after: page;"></div>

# Contact Eclypses

<img src="Eclypses.png" style="width:8in;"/>

<p align="center" style="font-weight: bold; font-size: 22pt;">For more information, please contact:</p>
<p align="center" style="font-weight: bold; font-size: 22pt;"><a href="mailto:info@eclypses.com">info@eclypses.com</a></p>
<p align="center" style="font-weight: bold; font-size: 22pt;"><a href="https://www.eclypses.com">www.eclypses.com</a></p>
<p align="center" style="font-weight: bold; font-size: 22pt;">+1.719.323.6680</p>

<p style="font-size: 8pt; margin-bottom: 0; margin: 300px 24px 30px 24px; " >
<b>All trademarks of Eclypses Inc.</b> may not be used without Eclypses Inc.'s prior written consent. No license for any use thereof has been granted without express written consent. Any unauthorized use thereof may violate copyright laws, trademark laws, privacy and publicity laws and communications regulations and statutes. The names, images and likeness of the Eclypses logo, along with all representations thereof, are valuable intellectual property assets of Eclypses, Inc. Accordingly, no party or parties, without the prior written consent of Eclypses, Inc., (which may be withheld in Eclypses' sole discretion), use or permit the use of any of the Eclypses trademarked names or logos of Eclypses, Inc. for any purpose other than as part of the address for the Premises, or use or permit the use of, for any purpose whatsoever, any image or rendering of, or any design based on, the exterior appearance or profile of the Eclypses trademarks and or logo(s).
</p>