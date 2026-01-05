薄桥化（Thin Bridge）重构规范（中文说明）
Minecraft Forge 1.8.9 + Sponge Mixin

## 为什么要做薄桥
- Mixin 会被合并进 MC 原始类，ZKM 混淆后类上下文变化会破坏解密 seed。
- 业务逻辑留在 Mixin 会让 ZKM 的控制流/字符串混淆影响到 MC 类，导致崩溃。
- 薄桥只做 ABI 级转发，Hook 才是安全可混淆的逻辑层。

结论：
- `mixin` 包 = ABI 桥接层（必须保持稳定、可读、无混淆）
- `hooks` 包 = 业务逻辑层（允许强混淆）

## Mixin 类规则
1) 所有 @Mixin 方法体只能一行：
```
HookClass.method(originalArgs);
```
或有返回值时：
```
return HookClass.method(originalArgs);
```
2) 注入点参数必须完全不变：
`method / at / ordinal / shift / slice / cancellable / remap`
3) 禁止：
- 任何逻辑 / if / for / lambda
- static {} 静态块
- 字符串/数字加密相关
- 集合、事件派发、状态管理
4) 状态：
- 禁止放在 Mixin
- 必须则用 @Unique 最小化
- 推荐放在 Hook 的 `Map<UUID|Entity, State>`
5) 字符串：
- Mixin 中尽量不写字面量
- 放到 Hook 或 Constants

## Accessor / Invoker 规则
- 只保留 interface
- 不允许 default 方法逻辑
- 访问私有字段/受保护方法时，优先通过 Accessor/Invoker

## Hook 类规则
- 普通 Java 类（无 @Mixin / @Shadow / @Accessor）
- 不引用 org.spongepowered.asm.mixin.*
- 方法尽量 static
- 负责全部业务逻辑

## 访问实例的方法（必要时）
如果 Hook 需要访问目标实例或受保护方法：
- 允许把 `self` 作为 Hook 的第一个参数传入
- 在 Hook 内部通过 Accessor/Invoker 操作私有字段/方法

示例：
```java
@Inject(method = "foo", at = @At("HEAD"))
private void onFoo(CallbackInfo ci) {
    FooHooks.onFoo((TargetClass) (Object) this, ci);
}
```
```java
public final class FooHooks {
    public static void onFoo(TargetClass self, CallbackInfo ci) {
        // 业务逻辑
    }
}
```

## 迁移步骤
1) 找到 Mixin 方法中的所有逻辑
2) 在 hooks 包创建同名 Hook 方法
3) 将逻辑全部移到 Hook
4) Mixin 方法体改为单行转发
5) 移除 Mixin 内的状态和字面量
6) Accessor/Invoker 只保留接口
7) 确认注入点参数完全一致

## 包结构建议
- `mixin/`      → Thin Bridge
- `hooks/`      → 业务逻辑
- `constants/`  → 字符串常量

## ZKM 混淆建议
hooks 包：
- 允许字符串/常量/控制流/异常混淆

mixin 包（必须排除）：
- 字符串加密
- 常量加密
- 控制流混淆
- 异常混淆
- 方法参数变更

原因：
- Mixin 会被合并进 MC 1.8.9 原始类
- 类上下文变化会破坏解密 seed
