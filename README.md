# Flow for Android

A library that makes it dead simple to manage complex flows of Android activities.

1. Define flows, their nodes and branching inside [Flow classes](#flow-classes) that resemble pseudocode.
2. Keep branching logic inside Flow activities and expose it via [Checklists](#checklists).
3. Use [Flow Manager](#flow-manager) to reduce the actual logic of navigating the flow to one-liners:

```kotlin
    FlowManager.start(flow, this) // Start a flow
    ...
    FlowManager.proceed(this) // Move to the next node in the flow
```

### Basic Concepts

A flow is graph whose nodes are Android activities or other flows, and branches represent conditions that govern transitions from one node to the other.

*Fragment-based flows __aren't supported__* as the complex Fragment lifecycle would make it very difficult to implement the current library functionality in a way that is consistent in all use cases and on all the Android devices.

Here's some library-specific terminology:
* A **node** represents an app screen, or an entry point to another flow.
* A **Relay** is the branching after a node, i.e it tells which node comes after the current one under given circumstances.
* A **Checklist** is the abstract API of a node, an interface consisting of parameter-less boolean methods that make reading the flow pathways simple and readable from inside the flow class, while encapsulating the relevant data and logic inside the nodes themselves.
* A **Post** is a glorified intent packed with additional data that can be used as a flow node, and that launches an Activity when invoked. Posts are [Launchable](flowLib/src/main/java/net/globulus/flow/Launchable.java), much like Flows themselves.

This may sound like a lot but it will all come together nicely when you look at the code sample in the following section.

Here's a visual representation of a flow with 4 posts and two relays (transitions), with one relay having a checklist that describes the branching:

![Flow diagram][imgs/flow_elements.jpg]


### Flow classes

A flow class defines the *layout of the flow*. The goal of the Flow class is for it to be readable basically like pseudocode, i.e someone who isn't necessarily proficient in Java or Kotlin should be able to convert the code file into a chart that graphically represents the same flow.

A flow class must extend [Flow](flowLib/src/main/java/net/globulus/flow/Flow.java).

Use the *put* method to add nodes to a Flow, as well as the branching that follows them. The ordering of nodes is technically not important, but it's a good practice to lay them out sequentially for readability purposes.

Each flow node must have a String **tag**. 

A null relay means that the flow will end or transition into another flow once we try to proceed from the current node.

Consider the following snippet that defines a login/register experience in the sample app:

```kotlin

```

### Posts

Posts are used to describe which Activity should be launched when a node is reached, and which parameters should be passed to it. Generally speaking, when adding nodes to a flow, you add either Posts or other Flows (although the Launchable interface allows for this rule to be applied to custom classes).

Internally, a Post is a wrapper around an Intent whose Builder exposes a lot of convenience methods useful for implementing behaviour specific for Activities used in Flows.

Here's a quick rundown of Post.Builder and its methods:
* The constructor takes a context and an Activity class, much like an Intent does.
* You can add any Intent flags via *addFlags*.
* *newTask* adds the flag that makes the launched Activity the sole one in the app.
* *rebase* and *rebaseWhen* tell the the flow should be (un)conditionally [rebased](#rebase) when the Activity is launched.
* *passIntentBundle* automatically adds the Intent extras of the current Activity to the one being launched (via the Post).
* Several overloaded variants of *putExtra* allow for easily passing data into the launched Activity, much like you'd do with an Intent.
    + Since Post are created when Flows are, the data passed via *putExtra* is determined when a Flow is created, and not when the Post is really invoked. To allow for evaluation of extras when the Post is actually launched, use the *putExtra* overload that takes a [ValueProducer](flowLib/src/main/java/net/globulus/flow/ValueProducer.java).

#### Flow Activities

All activities that are triggered by flow Posts should subclass [FlowActivity](flowLib/src/main/java/net/globulus/flow/FlowActivity.java). This allows flow activities to respond to flow events, such as the flow being [terminated](#terminate) or [rebased](#rebase). It also allows the FlowManager to know if a flow has been finished by backing out of it.

~~Technically, this is not a requirement, i.e Activities that are triggered by Posts aren't constrained to be subclasses of FlowActivity, but opting out of this~~

### Relays

Relays represent branchings after a node, telling the flow which way to go when the flow [proceeds](#proceed).

Code-wise, a [Relay](flowLib/src/main/java/net/globulus/flow/Relay.java) is a functional interface whose sole method, *nextPost*, returns a String representing the tag of the node the Flow should proceed to. It takes three parameters:

   * the current Flow *f*,
   * the current Activity *a*, and
   * the [checklist](#checklists) describing the branching conditions.
   
Due to the way FlowManager works, *a* and *c* are always going to point to the same instance, i.e the Activity is required to implement the Checklist the Relay is checking against.

Check out the following section for some code samples.

#### Checklists

Checklists are an abstract API that tell us how to navigate the flow from a certain node.

Checklists exist to force the developer to contain all the data and logic in that particular node, as opposed to leaking them into the Flow class itself, which would make it cluttered and defy the notion that a Flow class should be simple and almost read like pseudocode. In other words, we use Checklists to detach the flow branching rules (used in Flow class) from their implementation in the node Activity.

From the code perspective, a Checklist is just an interface that extends [Checklist](flowLib/src/main/java/net/globulus/flow/Checklist.java). Naturally, you can whatever you want in it, but ideally a Checklist should only contain *parameter-less methods that return a boolean*:

```kotlin
interface SignupChecklist : Checklist {
    fun isMinor(): Boolean
    fun hasPhoneNumber(): Boolean
}
```

Following this rule makes Relay implementation in Flow classes a lot cleaner:

```kotlin
...
{ f, a, c ->
    return when {
        c.isMinor() -> FlowConstants.GUARDIAN_EMAIL
        c.hasPhoneNumber() -> FlowConstants.SMS_CONSENTS
        else -> FlowConstants.ONBOARDING
    }
}
...
```

At the same time, the actual data and logic used to compute the checklist results are contained where they should be, in the Flow activity implementing the checklist:

```kotlin
...
override fun isMinor() = datePickerView.getDate().after(MINOR_DOB)

override fun isMinor() = !phoneNumberText.getText().toString().isEmpty()
...
```


#### Entry Relay

A Flow can specify an [EntryRelay](flowLib/src/main/java/net/globulus/flow/EntryRelay.java), which allows it to dynamically decide what should its origin node be.

EntryRelay takes three params - the flow, a context, and the bundle passed to the flow when it was started - and returns the origin node's tag based on that info.

An EntryRelay can be used to decide that a flow was wrongly started or that the caller flow should've reached its end by returning *null* as the origin tag, in case of which the flow won't be started at all.

#### Exit Relay

A Flow can specify an [ExitRelay](flowLib/src/main/java/net/globulus/flow/ExitRelay.java) in order to jump to a node in the case of flow finishing due to backing out. In order words, if you backtrack through the flow to its origin node, press back again, it will use the ExitRelay to figure out if it should jump to another node instead of finishing the flow.

An example use case for ExitRelay would be if you have a flow that always redirects, i.e going forward through the flow or backing out of it always takes you to a screen that doesn't correspond to the screen whence the flow was started.


### Flow manager

Although you can launch and monitor Flows directly, it's much more convenient to use the [FlowManager](flowLib/src/main/java/net/globulus/flow/FlowManager.java)'s *shared()* singleton. Basically, it allows you to easily perform the following tasks:

* Start a flow, with or without a bundle, with an option of returning a result.
* Move forward or backward within a flow without actually having to know anything about the ongoing flow, or its current state.
* Terminate a flow.
* Rebase a flow.
* Switch from one flow to another.

The FlowManager will make sure that all of these actions are valid and that they work as one would expect based on Flow classes, while also making them easy to use and not have to worry about flow state.

#### Flow bundles

Each flow has a bundle associated with it that's available to all of its nodes. Basically, whenever a Launchable (Post or another Flow) is launched within a Flow, it gets the current Flow bundle mixed in with other extras.

You may start a flow with a bundle, using *FlowManager#start(Flow, Context, Bundle)*, otherwise an empty bundle will be assigned to it. You may add 

#### Start

##### Returning results from Flows

One of the centerpieces of Android ecosystem is using Activities to return results to their caller Activities. Flows can be used to do exactly that.

#### Proceed

#### Terminate

#### Rebase

Rebasing makes a single node the current origin of the Flow, meaning that if you were to go back from it, you'd exit the Flow. Rebase should be used when you've 