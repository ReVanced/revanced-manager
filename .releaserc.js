module.exports = {
  "branches": [
    "main",
    {
      "name": "dev",
      "prerelease": true
    }
  ],
  "plugins": [
    [
      "@semantic-release/commit-analyzer", {
        "releaseRules": [
          { "type": "build", "scope": "Needs bump", "release": "patch" }
        ]
      }
    ],
    "@semantic-release/changelog",
    [
      "@semantic-release/release-notes-generator",
      {
        preset: "conventionalcommits",
        presetConfig: {
          types: [
            { type: "feat", section: "Features" },
            { type: "fix", section: "Bug Fixes" },
            { type: "docs", section: "Documentation" },
            { type: "style", section: "Styles" },
            { type: "refactor", section: "Code Refactoring" },
            { type: "perf", section: "Performance Improvements" },
            { type: "test", section: "Tests" },
            { type: "build", section: "Build System" },
            { type: "ci", section: "Continuous Integration" },
            { type: "chore", section: "Chores" },
            { type: "revert", section: "Reverts" },
          ]
        },
        writerOpts: {
          transform: (commit, context) => {
            if (commit.author.name === "semantic-release-bot") return;
            const types = {
              feat: "Features",
              fix: "Bug Fixes",
              docs: "Documentation",
              style: "Styles",
              refactor: "Code Refactoring",
              perf: "Performance Improvements",
              test: "Tests",
              build: "Build System",
              ci: "Continuous Integration",
              chore: "Chores",
              revert: "Reverts",
            }
            commit.type = types[commit.type];
            return commit;
          },
          commitPartial: "* {{#if scope}}**{{scope}}:** {{/if}}{{subject}} ([{{author.name}}]({{~@root.host}}/{{~@root.owner}}/{{~@root.repository}}/commit/{{hash}}))\n",
          mainTemplate: `
{{#each commitGroups}}
{{#if title}}
## {{title}}
{{/if}}
{{#each commits}}
{{> commit root=@root}}
{{/each}}
{{/each}}
          `
        }
      }
    ],
    [
      "@droidsolutions-oss/semantic-release-update-file",
      {
        "files": [
          {
            "path": ["pubspec.yaml"],
            "type": "flutter",
            "branches": ["main", "dev"]
          }
        ]
      }
    ],
    [
      "@semantic-release/exec",
      {
        "prepareCmd": "flutter build apk"
      }
    ],
    [
      "@semantic-release/git",
      {
        "assets": [
          "pubspec.yaml"
        ]
      }
    ],
    [
      "@semantic-release/github",
      {
        "assets": [
          {
            "path": "build/app/outputs/apk/release/revanced-manager*.apk"
          }
        ],
        "successComment": false
      }
    ],
    [
      "@saithodev/semantic-release-backmerge",
      {
        "backmergeBranches": [{"from": "main", "to": "dev"}],
        "clearWorkspace": true
      }
    ]
  ],


};
