# Whack

[![Build Status](https://github.com/igniterealtime/whack/actions/workflows/ci.yml/badge.svg)](https://github.com/igniterealtime/whack/actions) [![Project Stats](https://www.openhub.net/p/WhackAPI/widgets/project_thin_badge.gif)](https://www.openhub.net/p/WhackAPI)

Open Source XMPP (Jabber) component library for XMPP components. It allows you to create an XMPP component entity that
runs external to your XMPP server.

Whack is a Java library that easily allows the creation of external components that follow the
[XEP-0114: Jabber Component Protocol](http://www.xmpp.org/extensions/xep-0114.html).</p>

Components that were implemented as internal components of Openfire but do not make use of the internal
API of Openfire could be ported to Whack and run as external components.

Usage & Build
---------

The project requires Java 7, and is build with Apache Maven.

The primary ('core') artifact of this project is available as a Maven dependency. You can use it by including a snippet like
the one below to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.igniterealtime.whack</groupId>
    <artifactId>core</artifactId>
    <version>3.0.0</version>
</dependency>
```

(Please check for the version of the latest release, as we're likely to forget updating the example above when we make new releases).

To build the artifacts yourself, execute:

```bash
mvn clean package
```

Examples
--------

To illustrate the application of this project, two example modules are part of the project:

- [`sample/weather`](https://github.com/igniterealtime/Whack/tree/master/sample/weather)
- [`sample/weatherabstract`](https://github.com/igniterealtime/Whack/tree/master/sample/weatherabstract)

Both implement a basic component that can answer certain weather-related queries. Their implementation differs, to
illustrate how to use certain API endpoints provided by the Whack project.

Resources
---------

- Project homepage : https://www.igniterealtime.org/projects/whack/
- Bug Tracker: http://issues.igniterealtime.org/browse/WHACK

Ignite Realtime
===============

[Ignite Realtime] is an Open Source community composed of end-users and developers around the world who
are interested in applying innovative, open-standards-based Real Time Collaboration to their businesses and organizations.
We're aimed at disrupting proprietary, non-open standards-based systems and invite you to participate in what's already one
of the biggest and most active Open Source communities.

_Whack started to be implemented on November 2004 and was being used by Jive internally. The library was
always available from SVN and many people are using it. However, it was never released as a public library
until July 2008._

[Openfire]: https://www.igniterealtime.org/projects/openfire/
[Whack]: https://www.igniterealtime.org/projects/whack/
[Ignite Realtime]: http://www.igniterealtime.org
[XMPP (Jabber)]: http://xmpp.org/
