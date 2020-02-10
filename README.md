# GitHub Org Explorer

This package is my attempt solve a couple of my personal pain points
in using [GitHub Classroom](https://classroom.github.com) and GitHub Classroom Assistant.

After you log in, select an organzation. 

You can then use the regex filter to select a subset of the repos in the organzation after which you can either:

 1. Export - this will export the selected repos.
 2. Delete - delete the selected repos.
 
 
 Here's a video walkthrough [Video Link](https://www.youtube.com/watch?v=e-gNzuBG3mU&feature=youtu.be)
 
# Install notes for oauth:
 1. Go to settings from dropdown menu on right
 2. Develeopr settings from left menu
 3. OAuth apops from left menu
 3. New Oauth Appl (right hand side)
 4. name any url description https:/localhost
 5. Copy Client ID and client secret into the strings in src/app/main/keys.cljs
 
# Notes

When deleting, it takes a bit of time for the API to update. Right
now, this doesn't happen automatically. You should periodically hit
the refresh button to see the updates.

Likewise, it takes a bit of time to load a large organization - please be patient.




## How to Run
```
I had to upgrade my version of node to get this all working. I use nvm so used ~nvm install node~ to accomplish this. Then I did the rest:

npm install electron -g
npm install shadow-cljs -g
npm install

npm run dev
electron .
```

## Release
```
npm run build
electron-packager . GitHub-Org-Browser --platform=all --arch=x64
```

The above will build for alla vailable platforms. To just build one, specify platform:

 - darwin
 - linux
 - mas
 - win32
 
 
Arch can be ia32, x63, armv7l, arm64, or mips64el

The directories created by electorn-packager contain the full release - you can run or install from there.

You can always run the application from this directory after building via *electron .*

# More notes

I used the starter code from https://github.com/ahonn/shadow-electron-starter

