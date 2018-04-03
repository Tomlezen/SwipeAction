<img src="https://github.com/tomlezen/SwipeAction/tree/master/screenshot/media.gif" alt="arc" style="max-width:100%;">

## Gradle

```
com.tlz.swipeaction:swipeaction:0.0.2
```
## 属性说明

|     swipe_orientation     |      布局方向：horizontal-横向；vertical-纵向       |
| :-----------------------: | :-------------------------------------------------: |
|        swipe_mode         |  滑动模式：drawer-抽屉；smooth-平滑；parallax-视差  |
| swipe_parallax_multiplier |                视差模式下的视差系数                 |
|       swipe_enable        |                   开启或禁止滑动                    |
|      swipe_behavior       | 滑动行为：SwipeActionBehavior，SwipeDismissBehavior |
|     swipe_sensitivity     |                    滑动灵敏系数                     |
|   swipe_layout_gravity    |            子控件的布局位置：start；end             |
|      swipe_direction      |        滑动方向：any；endToStart；startToEnd        |
| swipe_start_max_distance  |            从开始到结束可滑动的最大距离             |
|  swipe_end_max_distance   |            从结束到开始可滑动的最大距离             |

## Groguard

```
-keep public class * extends com.tlz.swipeaction.SwipeBehavior
-keepclassmembers public class * extends com.tlz.swipeaction.SwipeBehavior{
    public <init> (android.content.Context, android.util.AttributeSet);
}
```
