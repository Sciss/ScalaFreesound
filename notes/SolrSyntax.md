# Logical operators

- `AND`, `OR`, grouping via parenthesis `(`, `)`.
- logical NOT is tricky. `NOT+2` doesn't work.
  Instead one has to write `(*:*+NOT+2)`
- inclusive ranges: `[<start> TO <stop>]` where
  either start and stop can be the wildcard `*`,
  e.g. `[* TO 10]` means less than or equal to ten.
  