# Flow for Android

A library that makes it dead simple to manage complex flows of Android activities, fully written in Kotlin.

1. Define flows, their nodes and branching inside [Flow classes](#flow-classes) that resemble pseudocode.
2. Keep branching logic inside Flow activities and expose it via [Checklists](#checklists).
3. Use [Flow Manager](#flow-manager) to reduce the actual logic of navigating the flow to one-liners:

```kotlin
    FlowManager.start(flow, this) // Start a flow
    ...
    FlowManager.proceed(this) // Move to the next node in the flow
```

Check out the [demo app](app) to see most of the basic concepts in action: flow starting and chaining, branchings, entry and exit relays, checklists, passing data around, and using flows to deliver results.

Check out the docs below for a more in-depth overview of the library terminology, architecture, and code samples. If you're a Kotlin user, be sure to check out the [DSL section](#dsl) with simpler and more concise syntax.


### Installation

Flow library is hosted on [Jitpack](https://jitpack.io/). To add it to your Android project, do the following:

```gradle
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
```gradle
dependencies {
   implementation 'com.github.globulus:easyflows:-SNAPSHOT'
}
```

### Sneak Peek

A checklist:
```kotlin
interface RegisterChecklist : Checklist {
    val isMinor: Boolean
}
```

A flow:
```kotlin
private const val ORIGIN = REGISTER

fun Context.registerFlow() = flow {
    origin(ORIGIN) { c, _, _ ->
        if (EasyPrefs.getAgreedToTermsOfUse(c))
            ORIGIN
        else
            TERMS_OF_USE
    }
    
    TERMS_OF_USE marks post(TermsOfUseActivity::class.java) followedBy { _, a ->
        a.finish()
        ORIGIN
    }
    
    REGISTER marks post(RegisterActivity::class.java) followedBy { _, a ->
        if (a.isMinor)
            PARENTAL_CONSENT
        else
            PURCHASE
    }

    PARENTAL_CONSENT marks post(ParentalConsentActivity::class.java) followedBy PURCHASE

    PURCHASE marks purchaseFlow(FlowConstants.Source.REGISTER)
}
```

Starting the flow:
```kotlin
register.setOnClickListener {
    FlowManager.start(registerFlow(), this)
}
```

And finally moving through it:
```kotlin
register.setOnClickListener {
    proceed()
}
```


### Basic Concepts

A flow is graph whose nodes are Android activities or other flows, and branches represent conditions that govern transitions from one node to the other.

*Fragment-based flows __aren't supported__* as the complex Fragment lifecycle would make it very difficult to implement the current library functionality in a way that is consistent in all use cases and on all the Android devices.

Here's some library-specific terminology:
* A **Node** represents an app screen, or an entry point to another flow.
* A **Relay** is the branching after a node, i.e it tells which node comes after the current one under given circumstances.
* A **Checklist** is the abstract API of a node, an interface consisting of parameter-less boolean methods that make reading the flow pathways simple and readable from inside the flow class, while encapsulating the relevant data and logic inside the nodes themselves.
* A **Post** is a glorified intent packed with additional data that can be used as a flow node, and that launches an Activity when invoked. Posts are [Launchable](flowLib/src/main/java/net/globulus/easyflows/Launchable.kt), much like Flows themselves.

This may sound like a lot but it will all come together nicely when you look at the code sample in the following section.

Here's a visual representation of a flow with 4 posts and two relays (transitions), with one relay having a checklist that describes the branching:

![Flow diagram][imgs/flow_elements.jpg]


### Flow classes

A flow class defines the *layout of the flow*. The goal of the Flow class is for it to be readable basically like pseudocode, i.e someone who isn't necessarily proficient in Java or Kotlin should be able to convert the code file into a chart that graphically represents the same flow.

A flow class must extend [Flow](flowLib/src/main/java/net/globulus/easyflows/Flow.kt).

Use the *put* method to add nodes to a Flow, as well as the branching that follows them. The ordering of nodes is technically not important, but it's a good practice to lay them out sequentially for readability purposes.

Each flow node must have a String **tag**. 

A null relay means that the flow will end or transition into another flow once we try to proceed from the current node.

Consider the following snippet that defines a register experience in the sample app. As you can see, it's dead easy to see which nodes exists in the flow, and how would the app move between them.

```kotlin
class RegisterFlow(packageContext: Context) : Flow(packageContext) {
    init {
        setOrigin(ORIGIN) { c, _, _ ->
            if (EasyPrefs.getAgreedToTermsOfUse(c))
                ORIGIN
            else
                FlowConstants.TERMS_OF_USE
        }

        put(FlowConstants.TERMS_OF_USE,
            Post.Builder(packageContext, TermsOfUseActivity::class.java)
                .build()
        ) { _, a ->
            a.finish()
            ORIGIN
        }

        put(FlowConstants.REGISTER,
            Post.Builder(packageContext, RegisterActivity::class.java)
                .build()
        ) { _, a ->
            if (a.isMinor)
                FlowConstants.PARENTAL_CONSENT
            else
                FlowConstants.PURCHASE
        }

        put(FlowConstants.PARENTAL_CONSENT,
            Post.Builder(packageContext, ParentalConsentActivity::class.java)
                .build()
        ) { _, _ -> FlowConstants.PURCHASE }

        put(FlowConstants.PURCHASE, PurchaseFlow(packageContext, FlowConstants.Source.REGISTER))
    }

    companion object {
        private const val ORIGIN = FlowConstants.REGISTER
    }
}
```

### Posts

Posts are used to describe which Activity should be launched when a node is reached, and which parameters should be passed to it. Generally speaking, when adding nodes to a flow, you add either Posts or other Flows (although the Launchable interface allows for this rule to be applied to custom classes).

Internally, a Post is a wrapper around an Intent whose Builder exposes a lot of convenience methods useful for implementing behaviour specific for Activities used in Flows.

Here's a quick rundown of Post.Builder and its methods:
* The constructor takes a package context and an Activity class, much like an Intent does.
* You can add any Intent flags via *addFlags*.
* *newTask* adds the flag that makes the launched Activity the sole one in the app.
* *rebase* and *rebaseWhen* tell the the flow should be (un)conditionally [rebased](#rebase) when the Activity is launched.
* *passIntentBundle* automatically adds the Intent extras of the current Activity to the one being launched (via the Post).
* Several overloaded variants of *putExtra* allow for easily passing data into the launched Activity, much like you'd do with an Intent.
    + Since Post are created when Flows are, the data passed via *putExtra* is determined when a Flow is created, and not when the Post is really invoked. To allow for evaluation of extras when the Post is actually launched, use the *putExtra* overload that takes a [ValueProducer](flowLib/src/main/java/net/globulus/easyflows/ValueProducer.kt).

#### Flow Activities

All activities that are triggered by flow Posts should subclass [FlowActivity](flowLib/src/main/java/net/globulus/easyflows/FlowActivity.kt). This allows flow activities to respond to flow events, such as the flow being [terminated](#terminate) or [rebased](#rebase). It also allows the FlowManager to know if a flow has been finished by backing out of it.

The *FlowActivity* class implements *Checklist* interface, saving you the trouble of manually implementing one for nodes that don't have a branching after them.

### Relays

Relays represent branchings after a node, telling the flow which way to go when the flow [proceeds](#proceed).

Code-wise, a [Relay](flowLib/src/main/java/net/globulus/easyflows/Relay.kt) is a functional interface whose sole method, *nextNode*, returns a String representing the tag of the node the Flow should proceed to. It takes three parameters:

   * the current Flow *f*,
   * the current Activity *a*, and
   * the [checklist](#checklists) describing the branching conditions.
   
Due to the way FlowManager works, *a* and *c* are always going to point to the same instance, i.e the Activity is required to implement the Checklist the Relay is checking against. Because of this, there's a convenience *put* overload that takes in a block of *(Flow, Activity)*, where Activity is also a Checklist. That example is used throughout the demo app. 

Check out the [following section](#checklists) for some code samples.

#### Checklists

Checklists are an abstract API that tell us how to navigate the flow from a certain node.

Checklists exist to force the developer to contain all the data and logic in that particular node, as opposed to leaking them into the Flow class itself, which would make it cluttered and defy the notion that a Flow class should be simple and almost read like pseudocode. In other words, we use Checklists to detach the flow branching rules (used in Flow class) from their implementation in the node Activity.

From the code perspective, a Checklist is just an interface that extends [Checklist](flowLib/src/main/java/net/globulus/easyflows/Checklist.kt). Naturally, you can whatever you want in it, but ideally a Checklist should only contain *parameter-less methods that return a boolean*, which in Kotlin are syntactically equal to a *Boolean val*:

```kotlin
interface SignupChecklist : Checklist {
    val isMinor: Boolean
    val hasPhoneNumber: Boolean
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
override val isMinor
    get() = datePickerView.getDate().after(MINOR_DOB)

override val hasPhoneNumber
    get() = !phoneNumberText.getText().toString().isEmpty()
...
```

#### Entry Relay

A Flow can specify an [EntryRelay](flowLib/src/main/java/net/globulus/easyflows/EntryRelay.kt), which allows it to dynamically decide what should its origin node be.

EntryRelay takes three params - the flow, a context, and the bundle passed to the flow when it was started - and returns the origin node's tag based on that info.

An EntryRelay can be used to decide that a flow was wrongly started or that the caller flow should've reached its end by returning *null* as the origin tag, in case of which the flow won't be started at all.

#### Exit Relay

A Flow can specify an [ExitRelay](flowLib/src/main/java/net/globulus/easyflows/ExitRelay.kt) in order to jump to a node in the case of flow finishing due to backing out. In order words, if you backtrack through the flow to its origin node, press back again, it will use the ExitRelay to figure out if it should jump to another node instead of finishing the flow.

An example use case for ExitRelay would be if you have a flow that always redirects, i.e going forward through the flow or backing out of it always takes you to a screen that doesn't correspond to the screen whence the flow was started.

### Flow manager

Although you can launch and monitor Flows directly, it's much more convenient to use the [FlowManager](flowLib/src/main/java/net/globulus/easyflows/FlowManager.kt) singleton. Basically, it allows you to easily perform the following tasks:

* [Start a flow](#start), with or without a bundle, with an [option of returning a result](#returning-results-from-flows).
* [Move forward or backward](#proceed) within a flow without actually having to know anything about the ongoing flow, or its current state.
* [Terminate](#terminate) a flow.
* [Rebase](#rebase) a flow.
* [Switch](#switch) from one flow to another.

The *FlowManager* will make sure that all of these actions are valid and that they work as one would expect based on Flow classes, while also making them easy to use and not have to worry about flow state.

#### Flow bundles

Each flow has a bundle associated with it that's available to all of its nodes. Basically, whenever a Launchable (Post or another Flow) is launched within a Flow, it gets the current Flow bundle mixed in with other extras.

You may start a flow with a bundle, using *FlowManager#start(Flow, Context, Bundle)*, otherwise an empty bundle will be assigned to it. You may add data to the flow bundle at any point using *Flow#addToFlowBundle(Bundle)*, and read that data using *Flow#flowBundle* or *FlowManager#currentBundle*.

##### BundleProducer

The [BundleProducer](#flowLib/src/main/java/net/globulus/easyflows/BundleProducer.kt) interface tells that an Activity can return a bundle of some kind. When an Activity that implements this interface is used to start a Flow or a Post, its bundle is automatically added to the parameter bundle of the Flow/Post being started. Check out the demo app PurchaseFlow for a sample usage.

#### Start

FlowManager can start a Flow using a number of methods:
* Without a bundle using *start(Flow, Context)*.
* With a initial bundle using *start(Flow, Context, Bundle)*.
* [For a result](#returning-results-from-flows) using *start(Flow, Activity, int)* or *start(Flow, Activity, Bundle, int)*.

##### Returning results from Flows

One of the centerpieces of Android ecosystem is using Activities to return results to their caller Activities. Flows can be used to do exactly that.

1. Start a flow from an Activity using one of two overloaded FlowManager *startForResult* methods, supplying a request code, as usual.
2. Move through the flows normally.
3. Once it's time to return a result, use *Flow#terminate(int, Intent)*, or *Flow#finishWithResult(int, Intent)* in a Relay, to return the result code and data. Note that calling *Flow#terminate()* or *FlowManager#terminate()* will return *Activity.RESULT_CANCELLED* and null data.
4. Receive the result in *onActivityResult* of the Activity that started the Flow in the first place.

#### Proceed

The beauty of the flow architecture is that its internal workings are fully encapsulated in the Flow classes and Checklists - app events that move the flow forward needn't know of its current nor its next state.

The *FlowManager#proceed(Activity)* method will move the current flow forward - to its next node, or its end, with or without returning a result:

```kotlin
 register.setOnClickListener {
    FlowManager.proceed(this)
}
```

In order for the *proceed* method to work, a [TagActivityMapper instance](flowLib/src/main/java/net/globulus/easyflows/TagActivityMapper.kt) must be set for the *FlowManager* singleton. Its purpose is to map Activity classes to their node tags. The *TagActivityMapper* should be set before you start a flow, ideally in your custom *Application#onCreate*, or the *onCreate* of your first Activity.

#### Terminate

Terminating a flow will kill all of its activities, as well as any other flows linked to it. You can terminate a flow directly using its *Flow#terminate(int, Intent)* method, or terminate the current flow with *FlowManager#terminate()*.

Flow's terminate method returns a null, allowing it to be incorportated into a Relay's return easily. This method is also used to specify [result that's returned from a flow](#returning-results-from-flows).

#### Rebase

Rebasing makes a single node the current origin of the Flow, meaning that if you were to go back from it, you'd exit the Flow. Rebase should be used when you've reached a point of no return in your flow, i.e when the user shouldn't be able to backtrack to previous screens in the flow.

Flows can be rebased manually via the *Flow#rebase(String)* or *FlowManager#rebase(String)* methods, although the preferred way is to set *rebase()* or *rebase(ValueProducer<Boolean>)* in your *Post.Builder*, and the Flow will automatically be rebased once this Post is started.

#### Switch

You can switch from one flow to another on the fly using *FlowManager#switchTo(Flow, Context, Bundle)* method. This will terminate the current flow and start the new one.

#### DSL

Everything seen thus far can be equally used from Java and Kotlin, but Flow ships with a small DSL that makes flow creating and management in Kotlin much simpler:

1. Define a new Flow on any Context with **flow**.
2. Use **origin** instead of *setOrigin*, and **exit** instead of *setExitRelay*.
3. *TAG* **marks** **post(CLASS)** { builderMethods() } **followedBy** RELAY.
    * Naturally, the **followedBy** part is optional as RELAY is optional.
    * You can just use a String instead of a RELAY.
4. Add to Flow bundle with **+=**, and retreive from it with **[]**.
5. Just invoke **proceed** on any FlowActivity instead of *FlowManager.proceed(this)*.

The [demo app](app) makes heavy use of DSL, be sure to check it out!


