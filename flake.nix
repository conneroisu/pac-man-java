{
  description = "Personal Website for Conner Ohnesorge";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    systems.url = "github:nix-systems/default";
    flake-utils = {
      url = "github:numtide/flake-utils";
      inputs.systems.follows = "systems";
    };
  };

  nixConfig = {
    extra-substituters = ''https://conneroisu.cachix.org'';
    extra-trusted-public-keys = ''conneroisu.cachix.org-1:PgOlJ8/5i/XBz2HhKZIYBSxNiyzalr1B/63T74lRcU0='';
    extra-experimental-features = "nix-command flakes";
  };

  outputs = inputs @ {
    self,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachSystem [
      "x86_64-linux"
      "i686-linux"
      "x86_64-darwin"
      "aarch64-linux"
      "aarch64-darwin"
    ] (system: let
      pkgs = import inputs.nixpkgs {
        inherit system;
        overlays = [];
      };
    in {
      devShells.default = let
        scripts = {
          dx = {
            exec = ''$EDITOR $REPO_ROOT/flake.nix'';
            description = "Edit flake.nix";
          };
          clean = {
            exec = ''${pkgs.git}/bin/git clean -fdx'';
            description = "Clean Project";
          };
          run = {
            exec = ''
              mkdir -p bin
              ${pkgs.jdk23}/bin/javac -d bin src/**/*.java
              ${pkgs.jdk23}/bin/java -cp bin ui.RunGame
            '';
            description = "Compile and run the PacMan game";
          };
        };

        # Convert scripts to packages
        scriptPackages =
          pkgs.lib.mapAttrsToList
          (name: script: pkgs.writeShellScriptBin name script.exec)
          scripts;
      in
        pkgs.mkShell {
          shellHook = ''
            export REPO_ROOT=$(git rev-parse --show-toplevel)

            # Print available commands
            echo "Available commands:"
            ${pkgs.lib.concatStringsSep "\n" (
              pkgs.lib.mapAttrsToList (
                name: script: ''echo "  ${name} - ${script.description}"''
              )
              scripts
            )}

            echo "Git Status:"
            ${pkgs.git}/bin/git status
          '';
          packages = with pkgs;
            [
              # Nix
              alejandra
              nixd
              statix
              deadnix

              # Java
              jdk23
              jdt-language-server
            ]
            ++ (with pkgs;
              lib.optionals stdenv.isDarwin [
              ])
            ++ (with pkgs;
              lib.optionals stdenv.isLinux [
              ])
            # Add the generated script packages
            ++ scriptPackages;
        };
    });
}
