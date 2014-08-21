resources
=========
[finagle](https://twitter.github.io/finagle/)<br>
[slick](http://slick.typesafe.com/doc/2.1.0/sql.html)

# insert new user
curl -v -H "Content-Type: application/json" -d "{"id": 1, "firstName": "Kevin", "lastName": "stanton", "age": 33}" -X POST "localhost:8080/persons"
