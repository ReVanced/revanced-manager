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
        "preset": "conventionalcommits",
        writerOpts: {
          commitPartial: "* {{subject}} ([{{author.name}}]({{~@root.host}}/{{~@root.owner}}/{{~@root.repository}}/commit/{{hash}}))\n",
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
