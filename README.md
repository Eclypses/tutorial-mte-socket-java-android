<center>
<img src="Eclypses.png" style="width:50%;"/>
</center>
<div align="center" style="font-size:40pt; font-weight:900; font-family:arial; margin-top:100px; " >Android Java Socket Tutorial</div>
<div align="center" style="font-size:28pt; font-family:arial; " >
MTE<sup>TM</sup> Implementation Tutorial </div>
<div align="center" style="font-size:15pt; font-family:arial; " >
Using MTE<sup>TM</sup> version 3.1.x</div>
<br><br><br>

[Introduction](#introduction)

[Socket Tutorial Server](#socket-tutorial-server)

[Socket Tutorial Client](#socket-tutorial-client)

<br><br><br>
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

In this tutorial we are creating an MTE Encoder and an MTE Decoder in the server as well as the client because we are sending secured messages in both directions. This is only needed when there are secured messages being sent from both sides, the server as well as the client. If only one side of your application is sending secured messages, then the side that sends the secured messages has to have an Encoder and the side receiving the messages needs a Decoder.

These steps should be followed on the server side as well as on the client side of the program.
<br><br><br>

# Socket Tutorial Server
This tutorial does not include a server side. No matter which tutorial you pick for the server side, you have to make sure that the server will listen to your computer's actual IP address, not "localhost". Making the server listen to any interface will also work. The Android emulator runs in a different local network which connects to your actual computer using NAT.
<br><br><br>

# Socket Tutorial Client
**IMPORTANT:**
Please note that the solution provided in this tutorial uses our Java MTE helper library. That library includes all the Java support classes which you need to interact with the MTE library. The helper library does not include the MTE binary libraries. You can either download the MTE library helper project and rebuild it including the binary library files which are part of your license or you can use the helper library as it is and add the binary libraries directly to this tutorial project (see below).<br>

Also note that this tutorial uses our Diffie-Hellman helper library to implement a Diffie-Hellman key exchange. The result of this key exchange is a "shared secret" which will be used as entropy for the encoder on the sender side and the decoder on the receiver side. The basic idea of a Diffie-Hellman key exchange is to exchange a secret without exchanging a secret. For a basic understanding of how this works please start reading here:<br>
https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange<br><br><br>
**Getting The Helper Libraries**<br><br>
These libraries are already included in this tutorial. You only need to download them if you want to study the source code, or - in the case of MTE - rebuild the library to include your licensed MTE binary libraries.<br><br>
The Java MTE Helper Library can be downloaded here:<br>
https://github.com/Eclypses/package-java-mte-android<br>
The Java Diffie-Hellman Helper Library can be downloaded here:<br>
https://github.com/Eclypses/package-mtesupport-ecdh<br><br><br>
**Preparing And Running The Tutorial**<br><br>
<ol>
<li>Open the project in Android Studio. Within the project root directory of <code>SocketClient</code> navigate to <i>app/libs</i> (create the directories if they don't exist). Add the *.aar files for the Diffie-Hellman and MTE helper libraries here.</li>
<br>
<li>If you want to directly include the binary MTE libraries in this tutorial, navigate to the <i>app/src/main/</i> directory (create directories as needed) of the <code>SocketClient</code> project. Create a <i>jniLibs</i> directory. Then create subdirectories for the platforms you want to support:
<br>  • <i>arm64-v8a/</i> for the 64-bit ARM platform
<br>  • <i>armeabi-v7a/</i> for the 32-bit ARM platform
<br>  • <i>x86_64/</i> for the 64-bit Intel X86-64 platform
<br>  • <i>x86/</i> for the 32-bit Intel X86 platform<br>
Finally copy the <code>libmtejni.so</code> files from the lib directories of the according MTE distribution archives to the directories you just created.<br>
Should you prefer to rebuild the Java MTE Library with your MTE binary libraries included, please refer to the build instructions in that project and then install the generated library as described in step #1.
</li>
<br>
<li>You will find all MTE related source in the <code>MyApplication</code> class. This class, based on Android's <code>Application</code> class, will ensure that MTE's configuration does not get destroyed and recreated every time the main activity gets pushed to the background. Depending on the variant of MTE (core, MKE, FLEN) you want to use, you will have to uncomment / comment the according lines in <code>MyApplication.java</code> - please note that you will have to change the source code in 2 places: the declaration and creation for the encoder and decoder pair.</li>
<br>
<li><code>setupMTE(byte[])</code> contains all the code necessary described below to execute the Diffie-Hellman key exchange as well as exchanging the nonces (generated by the server) and personalization strings (generated by the client). With all the initial values exchanged, it will get MTE up and running for sending (Encoder) and receiving (Decoder). <code>setupMTE()</code> initially gets called from <code>MainActivity</code>. Due to the enforced nature of all network communication being asynchronous, callbacks will be executed when answers arrive and these callbacks will then call <code>setupMTE()</code> again. A list of 7 easy to follow steps in <code>setupMTE()</code> will show you how to implement the whole process.</li>
<br>
<li>To ensure the MTE library is licensed correctly run the license check and also run the DRBG's self test. The LicenseCompanyName, and LicenseKey below should be replaced with your company’s MTE license information. If a demo or trial version is being used any value can be passed into those fields and it will work.</li>
<br>
<li>For ease of implementation we are using the default (parameterless) constructors. These constructors will use the available buildtime options or a reasonable set of default options in case you are using a MTE library which incorporates runtime options.</li>
<br>
<li>Create MTE Decoder Instance and MTE Encoder Instances as shown in <code>setupMTE()</code>.</li>
<br>
<li>The values for entropy, nonce and identifier values:</li>
The entropy must be treated like an encryption key. It is the result of the Diffie-Hellman key exchange. Nonce and personalization strings do not have to be kept secret and as shown in the tutorial, they are exchanged between client and server in the clear. It is good practice, however, to make each side dependent on the other side for one of the values. The tutorial shows this by making the server supply the nonces and making the client supply the personalization strings.
<br>
For the entropy, we have to determine the size of the allowed entropy value based on the drbg we have selected. A code sample below is included to demonstrate how to get these values. The Diffie-Hellman key exchange generates a shared secret with a size of 32 bytes which is sufficient for all DRBGs in MTE. If, however, your own implementation generates entropy in a different way, here is the code to figure out the valid size range:

```java
MteDrbg drbg = encoder.getDrbg();
int entropyMinBytes = MteBase.getDrbgsEntropyMinBytes(drbg);
long entropyMaxBytes = MteBase.getDrbgsEntropyMaxBytes(drbg);
```

<li> After having created encoder and decoder, instantiate them and cleanup secret values like Diffie-Hellman private keys and secrets, nonces and personalization strings as quickly as possible.</li><br>
<li>Finally, we need to add the MTE calls to encode and decode the messages that we are sending and receiving from the other side. (Ensure on the server side the Encoder is called to encode the outgoing text, then the Decoder is called to decode the incoming response.)</li>
<br>
<li>In <code>MyApplication.java</code> you will find 2 functions,

```java
byte[] encodeData(byte[])
byte[] decodeData(byte[])
```
which are called by the application's activity in order to encode outgoing data and decode incoming data.</li>
<br></ol>

***The Client side of the MTE Sockets Tutorial should now be ready for use on your device.\
Please note that this client tutorial requires at least Android 7 (API 24) in order to build and run successfully.***
<br><br><br>
<div style="page-break-after: always; break-after: page;"></div>

# Contact Eclypses

<p align="center" style="font-weight: bold; font-size: 20pt;">Email: <a href="mailto:info@eclypses.com">info@eclypses.com</a></p>
<p align="center" style="font-weight: bold; font-size: 20pt;">Web: <a href="https://www.eclypses.com">www.eclypses.com</a></p>
<p align="center" style="font-weight: bold; font-size: 20pt;">Chat with us: <a href="https://developers.eclypses.com/dashboard">Developer Portal</a></p>
<p style="font-size: 8pt; margin-bottom: 0; margin: 100px 24px 30px 24px; " >
<b>All trademarks of Eclypses Inc.</b> may not be used without Eclypses Inc.'s prior written consent. No license for any use thereof has been granted without express written consent. Any unauthorized use thereof may violate copyright laws, trademark laws, privacy and publicity laws and communications regulations and statutes. The names, images and likeness of the Eclypses logo, along with all representations thereof, are valuable intellectual property assets of Eclypses, Inc. Accordingly, no party or parties, without the prior written consent of Eclypses, Inc., (which may be withheld in Eclypses' sole discretion), use or permit the use of any of the Eclypses trademarked names or logos of Eclypses, Inc. for any purpose other than as part of the address for the Premises, or use or permit the use of, for any purpose whatsoever, any image or rendering of, or any design based on, the exterior appearance or profile of the Eclypses trademarks and or logo(s).
</p>