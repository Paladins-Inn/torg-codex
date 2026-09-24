# TORG-CODEX - Reference for the Torg Eternity RPG

> What man is a man who does not make the world better.
>
> -- Balian, Kingdom of Heaven

## Abstract
TORG-CODEX is software to help Torg Eternity RPG players.


## History
The Torg-Codex has been developed by Vilenius.
He shared the code because I promised to help to transform it to a more modern Java approach.
I failed miserably years ago.
This is a clean rewrite but using the data provided by Vilenius.

## License
The license for this software is GNU AGPL 3.0 or newer.

Some data is private property of Ulisses Spiele GmbH and other authors.
Only approved data is provided in this repository.
The reference installation of this software contains additional data which can't be distributed publicly.


## Architectural Principles

tl;dr (ok, only the bullshit bingo words):
- Immutable Objects
- Relying heavily on generated code
- 100% test coverage of human-generated code
- Every line of code not written is bug-free!

Code test coverage for human-generated code should be 100%, machine-generated code is considered bugfree until proven 
wrong. But every line that needs not be written is a bug-free line without the need to test it. So aim for not writing code.


## A note from the author
If someone is interested in getting it faster, we may team up. I'm open for that. But be warned: I want to do it 
_right_. So no shortcuts to get faster. And be prepared for some basic discussions about the architecture or software 
design :-).

---
Bensheim, 2026-04-05
