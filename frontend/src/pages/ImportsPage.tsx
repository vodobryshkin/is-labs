import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useImportWorkersMutation, useListImportsQuery } from '../api/api'
import type { ImportResult } from '../api/api'
import { errorText, fieldLabel } from '../api/errors'
import Pager from '../components/Pager'

const titles = { IN_PROGRESS: 'Выполняется', SUCCESS: 'Завершён', FAILED: 'Ошибка' }

const descriptions: Record<string, string> = {
    json: 'JSON-массив работников. Вложенные объекты — целиком, без ссылок по ID.',
    csv: 'CSV с заголовком. Вложенные объекты — через колонки с префиксами (coordinates_x, person_hair_color, organization_full_name и т.д.).',
}

const accepts: Record<string, string> = {
    json: '.json,application/json',
    csv: '.csv,text/csv',
}

function ImportsPage({ userId }: { userId: number }) {
    const [file, setFile] = useState<File | null>(null)
    const [format, setFormat] = useState<'json' | 'csv'>('json')
    const [page, setPage] = useState(1)
    const [pageSize, setPageSize] = useState(20)
    const [message, setMessage] = useState('')
    const input = useRef<HTMLInputElement>(null)
    const [upload, state] = useImportWorkersMutation()
    const history = useListImportsQuery({ viewer: userId, page, page_size: pageSize }, {
        refetchOnMountOrArgChange: true,
    })
    const failure = state.error && 'data' in state.error
        ? state.error.data as ImportResult | undefined : undefined
    const result = state.data ?? (failure?.status === 'FAILED' ? failure : undefined)

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!file || state.isLoading) return
        if (file.size > 10 * 1024 * 1024) {
            setMessage('Размер файла должен быть не больше 10 MiB.')
            return
        }
        setMessage('')
        setPage(1)
        const response = await upload({ file, format })
        if (response.data) {
            setFile(null)
            if (input.current) input.current.value = ''
        }
    }

    return (
        <>
            <h2>Импорт работников</h2>
            <p>{descriptions[format]} При ошибке весь файл откатывается. Максимум 10 MiB и 10000 записей.</p>
            <form onSubmit={submit}>
                <fieldset>
                    <legend>Формат</legend>
                    <label>
                        <input type="radio" name="format" value="json" checked={format === 'json'}
                               onChange={() => { setFormat('json'); setFile(null); state.reset(); if (input.current) input.current.value = '' }} />
                        JSON
                    </label>
                    <label>
                        <input type="radio" name="format" value="csv" checked={format === 'csv'}
                               onChange={() => { setFormat('csv'); setFile(null); state.reset(); if (input.current) input.current.value = '' }} />
                        CSV
                    </label>
                </fieldset>
                <label>
                    {format === 'json' ? 'JSON-файл' : 'CSV-файл'}
                    <input ref={input} type="file" accept={accepts[format]}
                           disabled={state.isLoading} onChange={(event) => {
                        setFile(event.target.files?.[0] ?? null)
                        setMessage('')
                        state.reset()
                    }} />
                </label>
                <button type="submit" disabled={!file || state.isLoading}>
                    {state.isLoading ? 'Импорт…' : 'Загрузить'}
                </button>
            </form>
            {message && <p role="alert">{message}</p>}
            {state.isLoading && <p role="status">Дождитесь завершения обработки файла.</p>}
            {result?.status === 'SUCCESS' && (
                <p role="status">Импорт №{result.operation_id} завершён. Добавлено: {result.added_count}.</p>
            )}
            {result?.status === 'FAILED' && (
                <div role="alert">
                    <p>Импорт №{result.operation_id} отклонён.
                        {result.record != null && ` Запись №${result.record}.`} Объекты из файла не добавлены.</p>
                    <p>{result.message}</p>
                    {!!result.errors?.length && <ul>{result.errors.map((issue, i) => (
                        <li key={i}>{fieldLabel(issue.field)}: {issue.message}</li>
                    ))}</ul>}
                </div>
            )}
            {state.error && !result && (
                <p role="alert">{errorText(state.error)} Обновите историю перед повторной отправкой.</p>
            )}
            <h3>История импорта</h3>
            <p>Пользователь видит свои операции, администратор — все.</p>
            <button type="button" disabled={history.isFetching} onClick={() => history.refetch()}>
                Обновить историю
            </button>
            {history.isLoading && <p>Загрузка…</p>}
            {history.error && <p role="alert">{errorText(history.error)}</p>}
            {history.data && (
                <>
                    <table>
                        <thead><tr><th>ID</th><th>Статус</th><th>Пользователь</th><th>Добавлено</th></tr></thead>
                        <tbody>{history.data.items.map((operation) => (
                            <tr key={operation.id}>
                                <td>{operation.id}</td>
                                <td>{titles[operation.status]}</td>
                                <td>{operation.username}</td>
                                <td>{operation.status === 'SUCCESS' ? operation.added_count : '—'}</td>
                            </tr>
                        ))}</tbody>
                    </table>
                    {history.data.total_items === 0 && <p>Операций пока нет.</p>}
                    <Pager page={page} pageSize={pageSize} totalItems={history.data.total_items}
                           isFetching={history.isFetching} onPage={setPage} onPageSize={(size) => {
                        setPageSize(size)
                        setPage(1)
                    }} />
                </>
            )}
        </>
    )
}

export default ImportsPage