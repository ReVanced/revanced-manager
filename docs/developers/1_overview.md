# 💁 Overview
Take a quick peak of how ReVanced Compose work.

## ⚙️ Technology
We use [Jetpack Compose](https://developer.android.com/jetpack/compose) to 
build beautiful and performant user interfaces using declarative programming. 
It provides a unified and efficient way of building UI that is well-integrated with the Android framework. 

## 🌲 Structure
We structure our code to use MVVM (Model-View-ViewModel) architecture for easier maintenance of the code. 

* **Model**: responsible for retrieving & storing data as well as performing calculations and other operations
* **View**: responsible for displaying the UI to the user, and send inputs to **ViewModel**
* **ViewModel**: responsible for receiving inputs from **View** and send it to **Model** then update the result

## 🧑‍💻 Code readability
In order to maintain readability of the code, It's highly recommend that you follow 
https://developer.android.com/jetpack/compose/api-guidelines style guide in order to maintain
consistent readability through out the codebase. 

## 🎨 Design language
We adopt the [Material Design 3](https://m3.material.io) as our design language to 
bring friendly, intuitive and colourful UI to our application ensuring that the 
app's user interface is consistent, customizable, accessible, and engaging for our users. 

## 📃 Commit message
At ReVanced, we follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0) 
format for our commit message. 

`type`(**optional: scope**): message

Example:
* `feat`: translation settings
* `fix`(**settings**): NullPointerException when exporting logs
* `refactor`: optimise sub-optimal code
* `perf`(**api**): reduces network latency
* `docs`(**developer**): concise wording
* `build`: bump version to v1.9.3
