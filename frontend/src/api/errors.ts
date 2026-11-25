import type { SerializedError } from '@reduxjs/toolkit'
import type { FetchBaseQueryError } from '@reduxjs/toolkit/query/react'

export interface ValidationIssue {
  field: string
  code: string
  message: string
}

type RequestError = FetchBaseQueryError | SerializedError | undefined

// Сервер присылает сообщения на английском, поэтому известные тексты переводятся здесь,
// а для остальных показывается общее описание по HTTP-статусу.
const messages: Record<string, string> = {
  'Username or password is incorrect.': 'Неверный логин или пароль.',
  'A user with this username already exists.': 'Пользователь с таким логином уже существует.',
  'The request body contains invalid JSON or a value of the wrong type.':
    'Запрос содержит некорректные данные.',
  'The request body must be a JSON object.': 'Запрос содержит некорректные данные.',
  'Another organization with this full_name already exists.':
    'Организация с таким названием уже существует: регистр, лишние пробелы и кавычки не учитываются.',
  'The coordinates cannot be deleted while workers reference them.':
    'Координаты нельзя удалить: их используют работники.',
  'The location cannot be deleted while addresses or persons reference it.':
    'Локацию нельзя удалить: её используют адреса или персоны.',
  'The address cannot be deleted while organizations reference it.':
    'Адрес нельзя удалить: его используют организации.',
  'The person cannot be deleted while a worker references it.':
    'Персону нельзя удалить: она привязана к работнику. Сначала отвяжите её в окне изменения работника.',
  'The organization cannot be deleted while workers reference it.':
    'Организацию нельзя удалить: в ней есть работники.',
  'Indexation would produce a salary outside the allowed range.':
    'Индексация отменена: зарплата хотя бы одного работника вышла бы за допустимые пределы.',
  'Parameter rating must be an int32 integer.': 'Рейтинг должен быть целым числом.',
  'Parameter status has an unsupported value.': 'Неизвестный статус.',
}

const objectNames: Record<string, string> = {
  Worker: 'работник',
  Coordinates: 'координаты',
  Location: 'локация',
  Address: 'адрес',
  Organization: 'организация',
  Person: 'персона',
}

function translateMessage(message: string): string | undefined {
  const notFound = message.match(/^(\w+) with id=(\d+) (does|do) not exist\.$/)
  if (notFound) {
    return `Не найден объект «${objectNames[notFound[1]] ?? notFound[1]}» с ID ${notFound[2]}.`
  }
  const referenced = message.match(/^Worker with id=(\d+) is referenced by a person and cannot be deleted\.$/)
  if (referenced) {
    return `Работника с ID ${referenced[1]} нельзя удалить: с ним связана персона. Сначала отвяжите персону в окне изменения работника.`
  }
  const assigned = message.match(/^Person with id=(\d+) is already assigned to another worker\.$/)
  if (assigned) {
    return `Персона с ID ${assigned[1]} уже привязана к другому работнику.`
  }
  const director = message.match(/^Organization with id=(\d+) already has a hired director\.$/)
  if (director) {
    return `В организации с ID ${director[1]} уже есть действующий директор.`
  }
  return messages[message]
}

const fieldNames: Record<string, string> = {
  $: 'Запрос',
  id: 'ID',
  name: 'Имя',
  salary: 'Зарплата',
  rating: 'Рейтинг',
  start_date: 'Дата начала работы',
  coordinates: 'Координаты',
  x: 'X',
  y: 'Y',
  z: 'Z',
  person: 'Персона',
  location: 'Местоположение',
  eye_color: 'Цвет глаз',
  hair_color: 'Цвет волос',
  birthday: 'Дата рождения',
  weight: 'Вес',
  nationality: 'Национальность',
  organization: 'Организация',
  organization_id: 'ID организации',
  full_name: 'Полное название',
  annual_turnover: 'Годовой оборот',
  employees_count: 'Количество сотрудников',
  type: 'Тип',
  official_address: 'Официальный адрес',
  official_address_id: 'ID официального адреса',
  postal_address: 'Почтовый адрес',
  postal_address_id: 'ID почтового адреса',
  street: 'Улица',
  coefficient: 'Коэффициент',
  town: 'Город',
}

export function fieldLabel(field: string): string {
  return field
    .split('.')
    .map((part) => fieldNames[part] ?? part)
    .join(' → ')
}

const statusMessages: Record<number, string> = {
  400: 'Некорректный запрос.',
  401: 'Требуется вход в систему.',
  403: 'Запрос отклонён проверкой CSRF. Повторите действие.',
  404: 'Объект не найден.',
  409: 'Операция конфликтует с текущими данными.',
  422: 'Проверьте введённые данные.',
}

const issueMessages: Record<string, string> = {
  MUST_NOT_BE_NULL: 'Поле обязательно для заполнения.',
  MUST_NOT_BE_EMPTY: 'Поле не может быть пустым.',
  OUT_OF_RANGE: 'Значение вне допустимого диапазона.',
  MUST_MATCH_PATTERN: 'Значение имеет недопустимый формат.',
  MUST_BE_POSITIVE: 'Значение должно быть больше нуля.',
  INVALID_COMBINATION: 'Недопустимое сочетание полей.',
  MUST_BE_GREATER_THAN_MINUS_954: 'Значение должно быть больше -954.',
  MUST_BE_GREATER_THAN_MINUS_392: 'Значение должно быть больше -392.',
}

export function errorText(error: RequestError): string {
  if (!error) {
    return ''
  }
  if (!('status' in error)) {
    return 'Неизвестная ошибка.'
  }
  if (error.status === 'FETCH_ERROR') {
    return 'Сервер недоступен. Проверьте, что бэкенд запущен.'
  }
  if (typeof error.status !== 'number') {
    return 'Некорректный ответ сервера.'
  }
  if (error.status === 401 || error.status === 403 || error.status === 422) {
    return statusMessages[error.status]
  }

  const message = (error.data as { message?: string } | null)?.message ?? ''
  return translateMessage(message) ?? statusMessages[error.status] ?? `Ошибка сервера (код ${error.status}).`
}

export function validationIssues(error: RequestError): ValidationIssue[] {
  if (error && 'status' in error && error.status === 422) {
    const issues = (error.data as { errors?: ValidationIssue[] } | null)?.errors ?? []
    return issues.map((issue) => ({
      ...issue,
      message: issueMessages[issue.code] ?? 'Недопустимое значение.',
    }))
  }
  return []
}

export function isUnauthorized(error: RequestError): boolean {
  return !!error && 'status' in error && error.status === 401
}

const importMessages: Record<string, string> = {
  'The file must contain at least one worker.': 'В файле нет ни одной записи для импорта.',
  'The file must not contain more than 10000 records.': 'В файле больше 10000 записей: уменьшите файл.',
  'The import file must not exceed 10 MiB.': 'Файл импорта не должен превышать 10 MiB.',
  'The import file is empty.': 'Файл импорта пуст.',
  'The import file could not be read.': 'Не удалось прочитать файл импорта.',
  'The CSV file could not be read.': 'Не удалось прочитать CSV-файл: проверьте формат и кодировку.',
  'The import record contains invalid fields.': 'Запись содержит некорректные поля.',
  'An import record must not be null.': 'Одна из записей импорта пустая.',
  'Failed to save the import. No objects were added.': 'Не удалось сохранить данные. Объекты из файла не добавлены.',
  'must be greater than 0': 'значение должно быть больше нуля',
  'must not be null': 'обязательное поле',
  'must not be empty': 'не может быть пустой строкой',
}

const issueCodes: Record<string, string> = {
  MUST_NOT_BE_NULL: 'обязательное поле',
  MUST_NOT_BE_EMPTY: 'не может быть пустой строкой',
  MUST_BE_POSITIVE: 'значение должно быть больше нуля',
  MUST_MATCH_PATTERN: 'значение содержит недопустимые символы',
  INVALID_COMBINATION: 'недопустимое сочетание полей',
  OUT_OF_RANGE: 'значение вне допустимого диапазона',
  REFERENCE_NOT_ALLOWED: 'ссылки по ID запрещены: укажите вложенный объект целиком',
}

export function messageText(message?: string): string {
  if (!message) {
    return ''
  }
  const unsupported = message.match(/^Unsupported import format: (.+)\.$/)
  if (unsupported) {
    return `Неподдерживаемый формат импорта: ${unsupported[1]}.`
  }
  const csvRow = message.match(/^Failed to parse CSV row (\d+)\.$/)
  if (csvRow) {
    return `Не удалось разобрать строку CSV №${csvRow[1]}: проверьте значения колонок.`
  }
  const missing = message.match(/^(\w+) with id=(\d+) does not exist\.$/)
  if (missing) {
    const subjects: Record<string, string> = {
      Organization: 'Организация',
      Person: 'Персона',
      Worker: 'Работник',
      Coordinates: 'Координаты',
      Address: 'Адрес',
      Location: 'Локация',
    }
    return `${subjects[missing[1]] ?? missing[1]} с ID ${missing[2]} не существует.`
  }
  return importMessages[message] ?? translateMessage(message) ?? message
}

export function issueText(issue: { code?: string; message: string }): string {
  return (issue.code && issueCodes[issue.code]) ?? messageText(issue.message)
}
