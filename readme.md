#SAMLv2 Authenticated application

I integrated the SAMLv2 spring security plugin in our application some time ago, 
and I must say that was a bit challenging at the beginning: it took me a while 
to have it up and running, because I felt the need to understand what the different pieces
where doing, how they where interacting, and how to choose between the options.

The doc of the plug in is actually very clear, but the style of writing was a bit
too much "manualistic" for me, and it really took me a while to understand how and
why i wanted to combine the pieces.

What follows is a bit of a reconstructed journal of my exploration, in the hope of
putting my notes out from draft. It will help me to keep these things in mind 
(and at hand). Hopefully can be of some use for someone else.

## The structure

We devs learn a lot from others' code, and with git we have the opportunity to snapshot
different versions in a common format. I will make a tag for every interesting step
and report what is done and why.

### 1. The starting step

We start from a pretty basic spring boot application with spring security, without database.
It has a login screen, some users and roles (not used), a public page (the index) 
and a secret page ("/secret"). There is no static resources. I use CDN, and i don't
need to talk about how to serve static resources with spring security.

There are also a couple of tests that ensure that login screen behaves as expected, 
the anonymous resource is available, the secret one needs login.



## Links and notable resources

- [Start up a Jade app with spring](http://josdem.io/techtalk/spring/spring_boot_jade/) (I prefer it to other techs for theses small projects)
- [Jade reference](http://jade-lang.com/reference) and [interactive doc](https://naltatis.github.io/jade-syntax-docs/)
