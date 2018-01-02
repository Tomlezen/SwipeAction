## Gradle

```
com.tlz.swipeaction:swipeaction:0.0.2
```
## Groguard

```
-keep public class * extends com.tlz.swipeaction.SwipeBehavior
-keepclassmembers public class * extends com.tlz.swipeaction.SwipeBehavior{
    public <init> (android.content.Context, android.util.AttributeSet);
}
```
