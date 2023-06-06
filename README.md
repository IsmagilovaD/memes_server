# Сервис для шаринга мемов - memes server

## Сервис предоставляет возможность:
- загружать файлы в формате **.jpg**, **.jpeg**, **.png**, **.gif** и **.svg**
- получать файлы с сервера по токену

### Для загрузки файла на сервер надо отправить http запрос типа:
```
POST http://localhost:8080/upload
Content-Type: multipart/form-data; boundary=WebAppBoundary

--WebAppBoundary
Content-Disposition: form-data; name="file"; filename="file.gif"

< example/file.gif
--WebAppBoundary--

```
В ответе сервер пришлет token в формате json
```
{
  "token": "eXaMpLeT0kEn"
}
```
### Далее с помощью этого токена можно получить мем обратно
```
POST http://localhost:8080/file
Content-Type: Application/json

{
  "token": "eXaMpLeT0kEn"
}
```

## Для запуска проекта, нужно настроить конфигурацию проекта
в файле **src/main/resources/application.conf** задать переменные для
- запуска сервера
- подключения к PostgreSQL
- путь, по которому должны храниться загруженные файлы.

Затем запустить FileServer используя sbt