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
        presetConfig: {
          types: [
            { type: "feat", section: "Features" },
            { type: "fix", section: "Bug Fixes" },
            { type: "docs", section: "Documentation" },
            { type: "style", section: "Styles" },
            { type: "refactor", section: "Code Refactoring" },
            { type: "perf", section: "Performance Improvements" },
            { type: "test", section: "Tests" },
            { type: "build", section: "Builds" },
            { type: "ci", section: "Continuous Integration" },
            { type: "chore", section: "Chores" },
            { type: "revert", section: "Reverts" },
          ],
        },
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
