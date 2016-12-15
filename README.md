# dorali

This is a tiny library for building transitive SQL and Elasticsearch
queries. It's really more of a design pattern than a feature complete
solution. 

For more info I wrote up a blog post about the design of the library
[here](https://edbabcock.com/composable-queries.html). It talks more
about the design pattern and the trade-offs for using it. There are also
some small SQL examples in the post.


## Recent Release
Latest Leiningen version:

[![Clojars Project](https://img.shields.io/clojars/v/com.greenyouse/dorali.svg)](https://clojars.org/com.greenyouse/dorali)


## Usage

The gist is that it's easy to use transitives to create extensible
database queries.

SQL is built on top of [Honey SQL](https://github.com/jkk/honeysql) and
uses it's data structure DSL.

```clj
(require '[dorali.sql.core :as sql])

((sql/query>
  {:select [:emp_id :fname :lname]
   :from [:employee]
   :where (term> := :lname (=> :surname))})
   {:surname \"smith\"})

=> {:select [:emp_id :fname :lname],
    :from [:employee],
    :where [:= :lname \"smith\"]}
```

Elasticsearch queries are written in a similar data structure DSL that
can be passed to a native or REST client.


```clj
(require '[dorali.es.core :as es])

((es/query>
    {:query
     {:match-all (constantly {})}
     :size (es/default> 10 :size)})
   {:size 5})

=> {:query
    {:match-all {}}
    :size 5}
```

## License

Copyright © 2016 Ed Babcock

Distributed under the Eclipse Public License either version 1.0
