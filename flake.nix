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

  outputs = inputs @ {flake-utils, ...}:
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
          mvn-clean = {
            exec = ''${pkgs.maven}/bin/mvn clean'';
            description = "Maven clean";
          };
          mvn-compile = {
            exec = ''${pkgs.maven}/bin/mvn compile'';
            description = "Maven compile";
          };
          mvn-package = {
            exec = ''${pkgs.maven}/bin/mvn package'';
            description = "Maven package";
          };
          mvn-run = {
            exec = ''${pkgs.maven}/bin/mvn exec:java'';
            description = "Run with Maven";
          };
          mvn-test = {
            exec = ''${pkgs.maven}/bin/mvn test'';
            description = "Run all tests with Maven";
          };
          lint = {
            exec = ''
              export REPO_ROOT=$(git rev-parse --show-toplevel)
              cd $REPO_ROOT
              
              # Create a reports directory
              mkdir -p reports
              
              # Header
              echo -e "\033[1m\033[34m============== Pac-Man Java Linting ==============\033[0m"
              
              # Run checkstyle
              echo -e "\n\033[1m\033[36mRunning Checkstyle checks...\033[0m"
              ${pkgs.maven}/bin/mvn checkstyle:check -q
              if [ $? -eq 0 ]; then
                echo -e "\033[32m✓ Checkstyle: No violations found\033[0m"
              else
                echo -e "\033[31m✗ Checkstyle: Violations found\033[0m"
              fi
              ${pkgs.maven}/bin/mvn checkstyle:checkstyle -q
              if [ -f "target/checkstyle-result.xml" ]; then
                cp target/checkstyle-result.xml reports/
              fi
              
              # Run PMD
              echo -e "\n\033[1m\033[36mRunning PMD analysis...\033[0m"
              ${pkgs.maven}/bin/mvn pmd:check -q
              if [ $? -eq 0 ]; then
                echo -e "\033[32m✓ PMD: No violations found\033[0m"
              else
                echo -e "\033[31m✗ PMD: Violations found\033[0m"
              fi
              ${pkgs.maven}/bin/mvn pmd:pmd -q
              if [ -f "target/pmd.xml" ]; then
                cp target/pmd.xml reports/
              fi
              
              # Summary
              echo -e "\n\033[1m\033[34m============== Linting Summary ==============\033[0m"
              
              # Count issues
              checkstyle_issues=0
              pmd_issues=0
              
              # Try to get Checkstyle count
              if [ -f "reports/checkstyle-result.xml" ]; then
                checkstyle_issues=$(grep "<error" reports/checkstyle-result.xml | wc -l)
              fi
              echo -e "Checkstyle issues: $checkstyle_issues"
              
              # Try to get PMD count
              if [ -f "reports/pmd.xml" ]; then
                pmd_issues=$(grep "<violation" reports/pmd.xml | wc -l)
              fi
              echo -e "PMD issues: $pmd_issues"
              
              echo -e "\nDetailed reports available in the 'reports' directory"
              
              # Return success regardless of findings
              exit 0
            '';
            description = "Run all configured linting tools";
          };
          format = {
            exec = ''
              export REPO_ROOT=$(git rev-parse --show-toplevel)
              find $REPO_ROOT -name "*.java" -type f | xargs ${pkgs.google-java-format}/bin/google-java-format --replace
            '';
            description = "Format Java code with Google Java Format";
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
              maven
              checkstyle
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
