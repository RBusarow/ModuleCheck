(window.webpackJsonp=window.webpackJsonp||[]).push([[10],{106:function(e,n,t){"use strict";t.d(n,"a",(function(){return p})),t.d(n,"b",(function(){return f}));var r=t(0),o=t.n(r);function i(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function a(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);n&&(r=r.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,r)}return t}function c(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?a(Object(t),!0).forEach((function(n){i(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):a(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function s(e,n){if(null==e)return{};var t,r,o=function(e,n){if(null==e)return{};var t,r,o={},i=Object.keys(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||(o[t]=e[t]);return o}(e,n);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(o[t]=e[t])}return o}var u=o.a.createContext({}),l=function(e){var n=o.a.useContext(u),t=n;return e&&(t="function"==typeof e?e(n):c(c({},n),e)),t},p=function(e){var n=l(e.components);return o.a.createElement(u.Provider,{value:n},e.children)},m={inlineCode:"code",wrapper:function(e){var n=e.children;return o.a.createElement(o.a.Fragment,{},n)}},d=o.a.forwardRef((function(e,n){var t=e.components,r=e.mdxType,i=e.originalType,a=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),p=l(t),d=r,f=p["".concat(a,".").concat(d)]||p[d]||m[d]||i;return t?o.a.createElement(f,c(c({ref:n},u),{},{components:t})):o.a.createElement(f,c({ref:n},u))}));function f(e,n){var t=arguments,r=n&&n.mdxType;if("string"==typeof e||r){var i=t.length,a=new Array(i);a[0]=d;var c={};for(var s in n)hasOwnProperty.call(n,s)&&(c[s]=n[s]);c.originalType=e,c.mdxType="string"==typeof e?e:r,a[1]=c;for(var u=2;u<i;u++)a[u]=t[u];return o.a.createElement.apply(null,a)}return o.a.createElement.apply(null,t)}d.displayName="MDXCreateElement"},77:function(e,n,t){"use strict";t.r(n),t.d(n,"frontMatter",(function(){return a})),t.d(n,"metadata",(function(){return c})),t.d(n,"rightToc",(function(){return s})),t.d(n,"default",(function(){return l}));var r=t(3),o=t(7),i=(t(0),t(106)),a={id:"configuration",sidebar_label:"Configuration"},c={unversionedId:"configuration",id:"version-0.10.0/configuration",isDocsHomePage:!1,title:"configuration",description:"` kotlin",source:"@site/versioned_docs/version-0.10.0/configuration.mdx",slug:"/configuration",permalink:"/ModuleCheck/docs/configuration",editUrl:"https://github.com/rbusarow/ModuleCheck/versioned_docs/version-0.10.0/configuration.mdx",version:"0.10.0",sidebar_label:"Configuration",sidebar:"version-0.10.0/Basics",previous:{title:"Quick Start",permalink:"/ModuleCheck/docs/"},next:{title:"Change Log",permalink:"/ModuleCheck/docs/changelog"}},s=[],u={rightToc:s};function l(e){var n=e.components,t=Object(o.a)(e,["components"]);return Object(i.b)("wrapper",Object(r.a)({},u,t,{components:n,mdxType:"MDXLayout"}),Object(i.b)("pre",null,Object(i.b)("code",Object(r.a)({parentName:"pre"},{className:"language-kotlin"}),'plugins {\n  id("com.rickbusarow.module-check") version "0.10.0"\n}\n\nmoduleCheck {\n\n  checks {\n    redundant.set(false)\n    disableAndroidResources.set(false)\n    disableViewBinding.set(false)\n  }\n\n  alwaysIgnore.set(setOf(":test:core-jvm", ":test:core-android"))\n  ignoreAll.set(setOf(":app_ble"))\n\n  additionalKaptMatchers.set(\n    listOf(\n      modulecheck.api.KaptMatcher(\n        name = "Roomigrant",\n        processor = "com.github.RickBusarow.Roomigrant:RoomigrantCompiler",\n        annotationImports = listOf(\n          "dev\\\\.matrix\\\\.roomigrant\\\\.\\\\*",\n          "dev\\\\.matrix\\\\.roomigrant\\\\.GenerateRoomMigrations",\n          "dev\\\\.matrix\\\\.roomigrant\\\\.rules\\\\.\\\\*",\n          "dev\\\\.matrix\\\\.roomigrant\\\\.rules\\\\.FieldMigrationRule",\n          "dev\\\\.matrix\\\\.roomigrant\\\\.rules\\\\.OnMigrationEndRule",\n          "dev\\\\.matrix\\\\.roomigrant\\\\.rules\\\\.OnMigrationStartRule"\n        )\n      ),\n      modulecheck.api.KaptMatcher(\n        name = "VMInject",\n        processor = "my-project.codegen.vminject:processor",\n        annotationImports = listOf(\n          "vminject\\\\.\\\\*",\n          "vminject\\\\.VMInject",\n          "vminject\\\\.VMInject\\\\.Source",\n          "vminject\\\\.VMInjectParam",\n          "vminject\\\\.VMInjectModule"\n        )\n      )\n    )\n  )\n}\n')))}l.isMDXComponent=!0}}]);