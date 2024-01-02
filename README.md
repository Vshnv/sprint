# Sprint
#### A simple functional-paradigmn language

## Defining a value
let [IDENTIFIER] = [EXPRESSION]

eg:
```js
let someThing = 1 + 1
```

## Lists
[Element1. Element2...]
eg:
```js
[1,2,3,4]
```

## Objects
{ [IDENTIFIER]: [EXPRESSION],...}
eg:
```js
{
  name: "John",
  age: 26
}
```

## Function
{(args...) [Expressions]}
eg:
```js
{(a, b)
  a + b
}
```

## Invocation
[function](params...) | [function][function definition]
eg:
```js
run(1,2,3)
run{(a, b)
  a + b
}
```






