
Year 2005:
    - BitKeeper charges. Need of a new VCS software
    - Requirements:
        - Speed
        - Simple Design
        - 1000+ parallel branches
        - Fully distributed
        - Large scope of files
Git
    Snapshots instead of base+offsets
    Self as a new file system with VCS feature
    Separate functions online/offline clearly
    Everything in Git is check-summed before stored & referenced by checksum. [Serves as a file system]
    States:
        - committed
        - modified
        - staged
    CLI vs GUI: Full command support is on CLI

First Time Setup
    /etc/gitconfig: Global config for all users on the system
    ~/.gitconfig[~/.config/git/config]["C:\\Users\\$USER\\.gitconfig"]: git --global command will modify this file
    git config --global user.name "Amos"
    git config --global user.email "zjy119@tamu.edu"
    git config --global core.editor emacs
    git config --list
    git help config

Basic Operations
    git init
    git clone https://xxx
    "Add files to Git"
    git add *.c
    git add .
    git commit [-m(message)]

    git status [-s(shrot)]
    git diff [Current diff Staged]
    git diff --staged [Staged diff HEAD]

    "Remove From Git"
    rm fileToDelete => Changes not staged for commit : fileToDelete
    git rm fileToDelete => Changes to be committed: fileToDelete
    
    "Maintain in FileSystem but not in Git"
    vim .gitignore

    "Git Log"
    git log 
    git log -p(details) [-n] [show diff on most recent n commits]
    git log --stat [abbreviated stats for each commit]
    git log --oneline [--since=2.weeks]

    "Undo Changes"
    git commit --amend [--no-e]
    git reset HEAD fileName [Unstage a file.]
    git checkout HEAD fileName [Revert file to HEAD state] [Dangerous]
    git reset HEAD~1 [Discard the most recent commit]

    "Remote"
    git remote [-v](verbose)
    git remote add remoteName remoteAddr
    git remote show remoteName 
    git remote rename oldName newName
    git remote rm remoteName

    git fetch remoteName

    "Tags"
    git tag [-l] 
    git tag -a v1.0 -m 'Version 1'
    git show v1.0

    "Alias"
    git config --global alias.co checkout
    git config --global alias.br branch
    git config --global alias.cm commit 
    git config --global alias.st status

Branches 
    git branch newBranch <=> git checkout -b newBranch
        "HEAD" pointer will always point to top of current branch 
    git checkout existingBranch 
    git merge localBranch
    git branch -d[D] localMergedBranch
    git rebase (new)master
    git push remoteName currentBranch:remoteBranch

    git checkout --track origin/linux
    [
        Switched to a new branch 'linux'
        Branch 'linux' set up to track remote branch 'linux' from 'origin'.
    ]
    git checkout -b localBranchName remote/remoteBranchName 
    git branch -u remote/remoteBranchName =>
        Set current branch to track remote branch 

    git pull = git fetch + git merge 
    "rebase VS merge: realility or function-oriented"
    git rebase -i HEAD~3 => Squash most recent 3 commits 


Git Tools

    "Stash"
    git stash [Push all staged files into a Stack]
        [
            stash@{0}:
            stash@{1}:
            stash@{2}:
        ]
    git stash list    
    git stash apply [stash@{2}]     
    git stash drop stash@{0}

    git stash pop = git stash apply + git stash drop 

    git stash branch testchanges [Not suggested]
        = git checkout -b testchanges + git stash pop


    "Cleaning"    
    git clean -f(force) -d(directory) => Remove all UNTRACKED files (and directories)
    git clean -n => Dry run 

    "RESET"
    git reset HEAD~
        -- soft => Only revert HEAD
        -- mixed => Revert HEAD, Index No change on FileSystem [default]
        -- hard => Revert HEAD, Index, FileSystem [Dangerous]

    git merge --abort
    git rebase --abort

    "Revert"
    git revert -m 1 HEAD 
    git revert ^M [Revert "Revert Merge"]





