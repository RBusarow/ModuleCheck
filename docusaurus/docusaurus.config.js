module.exports = {
  title: "ModuleCheck",
  tagline: "Fast dependency graph linting for Gradle projects",
  url: "https://rbusarow.github.io/",
  baseUrl: "/ModuleCheck/",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  favicon: "img/favicon.ico",
  organizationName: "rbusarow", // Usually your GitHub org/user name.
  projectName: "ModuleCheck", // Usually your repo name.
  themeConfig: {
    hideableSidebar: true,
    colorMode: {
      defaultMode: "light",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    announcementBar: {
      id: "supportus",
      content:
        '⭐️ If you like ModuleCheck, give it a star on <a target="_blank" rel="noopener noreferrer" href="https://github.com/rbusarow/ModuleCheck/">GitHub</a>! ⭐️',
    },
    navbar: {
      title: "ModuleCheck",
      //      logo: {
      //        alt: 'ModuleCheck Logo',
      //        src: 'img/logo.svg',
      //      },
      items: [
        {
          type: "doc",
          docId: "quickstart",
          label: "Basics",
          position: "left",
        },
        {
          type: "doc",
          docId: "rules/unused",
          label: "Rules",
          position: "left",
        },
        {
          type: "docsVersionDropdown",
          position: "right",
          dropdownActiveClassDisabled: true,
          dropdownItemsAfter: [
            {
              // to: "/versions",
              // label: "All versions",
            },
          ],
        },
        {
          label: "Twitter",
          href: "https://twitter.com/rbusarow",
          position: "right",
        },
        {
          href: "https://github.com/rbusarow/ModuleCheck/",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      copyright: `Copyright © ${new Date().getFullYear()} Rick Busarow, Built with Docusaurus.`,
    },
    prism: {
      theme: require("prism-react-renderer/themes/github"),
      darkTheme: require("prism-react-renderer/themes/dracula"),
      additionalLanguages: ["kotlin", "groovy"],
    },
  },
  presets: [
    [
      "@docusaurus/preset-classic",
      {
        docs: {
          sidebarPath: require.resolve("./sidebars.js"),
          // Please change this to your repo.
          editUrl: "https://github.com/rbusarow/ModuleCheck/",
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl: "https://github.com/rbusarow/ModuleCheck/",
        },
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      },
    ],
  ],
};
